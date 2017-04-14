package com.opendoorlogistics.territorium.utils;

import java.awt.Color;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.Location;
import com.opendoorlogistics.territorium.problem.location.XYLocation;

/**
 * Only supports XY at the moment...
 * @author Phil
 *
 */
public class SVGWriter {
	private StringBuilder builder = new StringBuilder();
	private int len;
	
	public SVGWriter(int len){
		this.len = len;
	}
	
	public String getSVG(){
		return builder.toString();
	}
	
	public static String circle(int cx, int cy,int radius, String colourHex){
		return "\t<circle cx=\""+cx+"\" cy=\"" + cy + "\" r=\"" + radius + "\" stroke=\"black\" stroke-width=\"1\" fill=\"" + colourHex+"\" />";		
	}

	public static String exportXYProblem(int len, Problem problem,ImmutableSolution sol){
		return exportXYProblem(len, problem, sol.getClusterCentres(), sol.getCustomersToClusters());
	}

	public static class XYCoordTransformer{
		private int len;
		private double multiplier;
		private DoubleSummaryStatistics xstats = new DoubleSummaryStatistics();
		private DoubleSummaryStatistics ystats = new DoubleSummaryStatistics();

		public XYCoordTransformer(int len, Problem ...problems){
			this.len = len;
			
			for(Problem problem : problems){
				List<Location> allLocs = Problem.getAllLocations(problem);
				for(Location l:allLocs){
					XYLocation xy=(XYLocation)l;
					xstats.accept(xy.getX());
					ystats.accept(xy.getY());
				}				
			}
			
			double range = Math.max(xstats.getMax()-xstats.getMin(), ystats.getMax()-ystats.getMin());
			if(range==0){
				range = 1;
			}
			
			multiplier = 1.0/range;
		}
		
		int transform(double x, double min){
			return (int) Math.round((x - min)*len*multiplier);
		}
		public int cx(double x){
			return transform(x, xstats.getMin());
		}
		
		public int cy(double y){
			return transform(y, ystats.getMin());
		}
	}
	
	public static String exportXYProblem(int len, Problem problem,Location [] centres , int [] assignments){
		SVGWriter writer = new SVGWriter(len);

		writer.addHTMLHeader();

		writer.addLine("<section>");
		writer.addLine("<h1>Map</h1>");
		writer.addXYProblemSVG( problem, centres, assignments);
		writer.addLine("</section>");
		
		writer.addLine("<section>");
		writer.addLine("<h1>Quantities</h1>");
		writer.addSVGQuantityBarchart(new MutableSolution(problem, assignments));
		writer.addLine("</section>");
				
		writer.addHTMLFooter();
		return writer.getSVG();
	}

	public void addXYProblemSVG(Problem problem, Location[] centres, int[] assignments) {
		XYCoordTransformer transformer = new XYCoordTransformer(len,problem);
		
		// create a random colour for each territory
		String[] colors = createRandomColours( problem.getClusters().size());

		addSVGHeader();
		for(int i =0 ; i<centres.length ; i++){
			XYLocation loc = (XYLocation)centres[i];			
			addLine(SVGWriter.circle(transformer.cx(loc.getX()), transformer.cy(loc.getY()),8, colors[i]));
		}
		for(int i =0 ; i<assignments.length ; i++){
			Customer customer = problem.getCustomers().get(i);
			XYLocation loc = (XYLocation)customer.getLocation();
			addLine(SVGWriter.circle(transformer.cx(loc.getX()), transformer.cy(loc.getY()),4, colors[assignments[i]]));
		}
		addSVGFooter();
	}

	public void addSVGQuantityBarchart(ImmutableSolution solution){
		int nclust=solution.getProblem().getClusters().size();
		String [] colours = createRandomColours(nclust);
		addSVGHeader();
		
		double maxQuant=0;
		for(int i =0 ; i<nclust;i++){
			maxQuant = Math.max(solution.getClusterQuantity(i), maxQuant);
		}
		if(maxQuant==0){
			maxQuant =1 ;
		}

		
		if(nclust>0){
			double heightPerCluster = (double)len/nclust;
			for(int i =0 ; i<nclust;i++){
				int width =(int) Math.ceil(len * solution.getClusterQuantity(i)/maxQuant);
				int y0 = (int)Math.round(i*heightPerCluster);
				int y1 = y0 +(int)Math.round(0.67*heightPerCluster);
				if(y1==y0){
					y1++;
				}
				int height =y1-y0;
				builder.append("\t<rect x=\"0\" y=\"" +y0 + "\" width=\"" + width+ "\" height=\"" + height + "\"  fill=\"" + colours[i]+"\"></rect>");
				builder.append(System.lineSeparator());
			}
			
		}
		
		addSVGFooter();
	}
	
	public void addLine(String l){
		builder.append(l);
		builder.append(System.lineSeparator());
	}
	
	public static String[] createRandomColours(int ncolours) {
		Random random = new Random(123);
		String [] colors = new String[ncolours];
		for(int i =0 ; i<ncolours ; i++){
			Color col = new Color((float)random.nextDouble(),(float) random.nextDouble(), (float)random.nextDouble());
			colors[i] = String.format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue());
		}
		return colors;
	}

	public void addFooter() {
		addSVGFooter();
		addHTMLFooter();
	}

	public void addHTMLFooter() {
		builder.append("</body></html>");
		builder.append(System.lineSeparator());
	}

	public void addSVGFooter() {
		builder.append("</svg>");
		builder.append(System.lineSeparator());
	}

	public void addHeader() {
		addHTMLHeader();
		addSVGHeader();
	}
	
	public void addHeader(String title,int level){
		if(level < 1 || level > 6){
			throw new IllegalArgumentException("Heading must be between 1 and 6");
		}
		builder.append("<h" + level + ">" + title + "</h" + level + ">");
		builder.append(System.lineSeparator());
	}

	public void addHTMLHeader() {
		builder.append("<html><body>");
		builder.append(System.lineSeparator());
	}

	public void addSVGHeader() {
		builder.append("<svg width=\"" + len + "\" height=\"" + len + "\">");
		builder.append(System.lineSeparator());
	}
}
