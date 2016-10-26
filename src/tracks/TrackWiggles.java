package tracks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;
import org.broad.igv.tdf.TDFGroup;
import org.broad.igv.tdf.TDFReader;
import org.broad.igv.tdf.TDFUtils;
import org.broad.igv.util.ResourceLocator;

import utils.IOUtils;
import utils.BedLine;
import utils.BedLineCodec;
import com.google.common.base.Joiner;

import exceptions.InvalidRecordException;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.readers.TabixReader;
import htsjdk.tribble.readers.TabixReader.Iterator;
import htsjdk.tribble.util.TabixUtils;
import samTextViewer.GenomicCoords;
import samTextViewer.Utils;

/** Process wiggle file formats. Mostly using IGV classes. 
 * bigBed, bigWig, */
public class TrackWiggles extends Track {

	// private double scorePerDot;
	private List<ScreenWiggleLocusInfo> screenWiggleLocusInfoList;
	private int bdgDataColIdx= 4; 
	private BBFileReader bigWigReader;
	
	
	/* C o n s t r u c t o r s */

	/**
	 * Read bigWig from local file or remote URL.
	 * @param filename Filename or URL to access 
	 * @param gc Query coordinates and size of printable window 
	 * @throws IOException 
	 * @throws InvalidRecordException */
	public TrackWiggles(String filename, GenomicCoords gc, int bdgDataColIdx) throws IOException, InvalidRecordException{

		this.setGc(gc);
		this.setFilename(filename);
		this.bdgDataColIdx= bdgDataColIdx;

		if(Utils.getFileTypeFromName(this.getFilename()).equals(TrackFormat.BIGWIG)){
			this.bigWigReader=new BBFileReader(this.getFilename()); // or url for remote access.
			if(!this.bigWigReader.getBBFileHeader().isBigWig()){
				throw new RuntimeException("Invalid file type " + this.getFilename());
			}
		}
		
		this.update();
		
	};
	

	/*  M e t h o d s  */
	
	public void update() throws IOException, InvalidRecordException {

		if(this.bdgDataColIdx < 4){
			System.err.println("Invalid index for bedgraph column of data value. Resetting to 4. Expected >=4. Got " + this.bdgDataColIdx);
			this.bdgDataColIdx= 4;
		}

		if(Utils.getFileTypeFromName(this.getFilename()).equals(TrackFormat.BIGWIG)){
			
			bigWigToScores(this.bigWigReader);
			
		} else if(Utils.getFileTypeFromName(this.getFilename()).equals(TrackFormat.TDF)){
			
			this.updateTDF();
			
		} else if(Utils.getFileTypeFromName(this.getFilename()).equals(TrackFormat.BEDGRAPH)){

			// FIXME: Do not use hardcoded .samTextViewer.tmp.gz!
			if(Utils.hasTabixIndex(this.getFilename())){
				bedGraphToScores(this.getFilename());
			} else if(Utils.hasTabixIndex(this.getFilename() + ".samTextViewer.tmp.gz")){
				bedGraphToScores(this.getFilename() + ".samTextViewer.tmp.gz");
			} else {
				blockCompressAndIndex(this.getFilename(), this.getFilename() + ".samTextViewer.tmp.gz", true);
				bedGraphToScores(this.getFilename() + ".samTextViewer.tmp.gz");
			}
			
		} else {
			throw new RuntimeException("Extension (i.e. file type) not recognized for " + this.getFilename());
		}
		this.setYLimitMin(this.getYLimitMin());
		this.setYLimitMax(this.getYLimitMax());
	}

	private void updateTDF(){
		
		this.screenWiggleLocusInfoList= 
				TDFUtils.tdfRangeToScreen(this.getFilename(), this.getGc().getChrom(), 
						this.getGc().getFrom(), this.getGc().getTo(), this.getGc().getMapping());
		
		ArrayList<Double> screenScores= new ArrayList<Double>();
		for(ScreenWiggleLocusInfo x : screenWiggleLocusInfoList){
			screenScores.add((double)x.getMeanScore());
		}
		if(this.isRpm()){
			screenScores= this.normalizeToRpm(screenScores);
		}
		this.setScreenScores(screenScores);	

	}
	
	@Override
	public String printToScreen(){
	
		if(this.getyMaxLines() == 0){return "";}
		TextProfile textProfile= new TextProfile(this.getScreenScores(), this.getyMaxLines(), this.getYLimitMin(), this.getYLimitMax());
		
		ArrayList<String> lineStrings= new ArrayList<String>();
		for(int i= (textProfile.getProfile().size() - 1); i >= 0; i--){
			List<String> xl= textProfile.getProfile().get(i);
			lineStrings.add(StringUtils.join(xl, ""));
		}
		String printable= Joiner.on("\n").join(lineStrings);
		if(!this.isNoFormat()){
			printable= "\033[48;5;231;" + Utils.ansiColorCodes().get(this.getTitleColour()) + "m" + printable + "\033[48;5;231m";
		}
		return printable;
	}
	
	/**
	 * Block compress input file and create associated tabix index. Newly created file and index are
	 * deleted on exit if deleteOnExit true.
	 * @throws IOException 
	 * @throws InvalidRecordException 
	 * */
	private void blockCompressAndIndex(String in, String bgzfOut, boolean deleteOnExit) throws IOException, InvalidRecordException {
				
		File inFile= new File(in);
		File outFile= new File(bgzfOut);
		
		LineIterator lin= IOUtils.openURIForLineIterator(inFile.getAbsolutePath());

		BlockCompressedOutputStream writer = new BlockCompressedOutputStream(outFile);
		long filePosition= writer.getFilePointer();
				
		TabixIndexCreator indexCreator=new TabixIndexCreator(TabixFormat.BED);
		BedLineCodec bedCodec= new BedLineCodec();
		while(lin.hasNext()){
			String line = lin.next();
			if ( !this.isValidBedGraphLine(line.trim()) ) {
				System.err.println("\nInvalid record found: " + line + "\n");
				throw new InvalidRecordException();
			}			
			BedLine bed = bedCodec.decode(line);
			if(bed==null) continue;
			writer.write(line.getBytes());
			writer.write('\n');
			indexCreator.addFeature(bed, filePosition);
			filePosition = writer.getFilePointer();
		}
		writer.flush();
				
		File tbi= new File(bgzfOut + TabixUtils.STANDARD_INDEX_EXTENSION);
		if(tbi.exists() && tbi.isFile()){
			writer.close();
			throw new RuntimeException("Index file exists: " + tbi);
		}
		Index index = indexCreator.finalizeIndex(writer.getFilePointer());
		index.writeBasedOnFeatureFile(outFile);
		writer.close();
		
		if(deleteOnExit){
			outFile.deleteOnExit();
			File idx= new File(outFile.getAbsolutePath() + TabixUtils.STANDARD_INDEX_EXTENSION);
			idx.deleteOnExit();
		}
	}

	@Override
	public String getTitle(){

		if(this.isHideTitle()){
			return "";
		}
		
		double[] rounded= Utils.roundToSignificantDigits(this.getMinScreenScores(), this.getMaxScreenScores(), 2);
		
		String ymin= this.getYLimitMin().isNaN() ? "auto" : this.getYLimitMin().toString();
		String ymax= this.getYLimitMax().isNaN() ? "auto" : this.getYLimitMax().toString();
		
		String xtitle= this.getTrackTag() 
				+ "; ylim[" + ymin + " " + ymax + "]" 
				+ "; range[" + rounded[0] + " " + rounded[1] + "]\n";
		return this.formatTitle(xtitle);
	}
	
	/** Return true if line looks like a valid bedgraph record  
	 * */
	private boolean isValidBedGraphLine(String line){
		
		if(line.trim().startsWith("#") || line.trim().startsWith("track ")){
			return true;
		}
		
		String[] bdg= line.split("\t");
		if(bdg.length < 4){
			return false;
		}
		try{
			Integer.parseInt(bdg[1]);
			Integer.parseInt(bdg[2]);
			Double.parseDouble(bdg[this.bdgDataColIdx - 1]);
		} catch(NumberFormatException e){
			return false;
		}
		return true;
	}
	
	/** Populate object using bigWig data */
	private void bigWigToScores(BBFileReader reader){

		// List of length equal to screen size. Each inner map contains info about the screen locus 
		List<ScreenWiggleLocusInfo> screenWigLocInfoList= new ArrayList<ScreenWiggleLocusInfo>();
		for(int i= 0; i < getGc().getUserWindowSize(); i++){
			screenWigLocInfoList.add(new ScreenWiggleLocusInfo());
		}
		
		BigWigIterator iter = reader.getBigWigIterator(getGc().getChrom(), getGc().getFrom(), getGc().getChrom(), getGc().getTo(), false);
		while(iter.hasNext()){
			WigItem bw = iter.next();
			for(int i= bw.getStartBase(); i <= bw.getEndBase(); i++){
				int idx= Utils.getIndexOfclosestValue(i, getGc().getMapping()); // Where should this position be mapped on screen?
				screenWigLocInfoList.get(idx).increment(bw.getWigValue());
			} 
		}
		ArrayList<Double> screenScores= new ArrayList<Double>();
		for(ScreenWiggleLocusInfo x : screenWigLocInfoList){
			screenScores.add((double)x.getMeanScore());
		}
		this.setScreenScores(screenScores);		
	}
	
	/** Get values for bedgraph
	 * @throws InvalidRecordException 
	 * */
	private void bedGraphToScores(String fileName) throws IOException, InvalidRecordException{
		
		List<ScreenWiggleLocusInfo> screenWigLocInfoList= new ArrayList<ScreenWiggleLocusInfo>();
		for(int i= 0; i < getGc().getUserWindowSize(); i++){
			screenWigLocInfoList.add(new ScreenWiggleLocusInfo());
		}
		
		try {
			TabixReader tabixReader= new TabixReader(fileName);
			Iterator qry= tabixReader.query(this.getGc().getChrom(), this.getGc().getFrom()-1, this.getGc().getTo());
			while(true){
				String q = qry.next();
				if(q == null){
					break;
				}
				if ( !this.isValidBedGraphLine(q) ) {
					System.err.println("\nInvalid record found: " + q + "\n");
					throw new InvalidRecordException();
				}
				String[] tokens= q.split("\t");
				int screenFrom= Utils.getIndexOfclosestValue(Integer.valueOf(tokens[1])+1, this.getGc().getMapping());
				int screenTo= Utils.getIndexOfclosestValue(Integer.valueOf(tokens[2]), this.getGc().getMapping());
				float value= Float.valueOf(tokens[this.bdgDataColIdx-1]);
				for(int i= screenFrom; i <= screenTo; i++){
					screenWigLocInfoList.get(i).increment(value);
				}
			}
		} catch (IOException e) {			
			e.printStackTrace();
			System.err.println("Could not open tabix file: " + fileName);
			System.err.println("Is the file sorted and indexed? After sorting by position (sort e.g. -k1,1 -k2,2n), compress with bgzip and index with e.g.:");
			System.err.println("\nbgzip " + fileName);
			System.err.println("tabix -p bed " + fileName + "\n");
		}
		ArrayList<Double> screenScores= new ArrayList<Double>();
		for(ScreenWiggleLocusInfo x : screenWigLocInfoList){
			screenScores.add((double)x.getMeanScore());
		}
		this.setScreenScores(screenScores);
		return;
	}
	
	private ArrayList<Double> normalizeToRpm(ArrayList<Double> yValues){
		ArrayList<Double> rpmed= new ArrayList<Double>();
		String x= this.getAttributesFromTDF("totalCount");
		if(x == null){
			System.err.println("Warning: Cannot get total counts for " + this.getFilename());
			return yValues;
		}
		Integer totalCount= Integer.parseInt(x);
		for(int i= 0; i < yValues.size(); i++){
			rpmed.add(yValues.get(i) / totalCount * 1000000.0);
		}
		return rpmed;
	}
	
	private String getAttributesFromTDF(String attr){
		
		String path= this.getFilename();
		
		try{
			ResourceLocator resourceLocator= new ResourceLocator(path);
			TDFReader reader= new TDFReader(resourceLocator);
			TDFGroup rootGroup= reader.getGroup("/");
			return rootGroup.getAttribute(attr);
		} catch(Exception e){
			return null;
		}
	}
	
	
	/*   S e t t e r s   and   G e t t e r s */
	
	protected int getBdgDataColIdx() { return bdgDataColIdx; }
	protected void setBdgDataColIdx(int bdgDataColIdx) { this.bdgDataColIdx = bdgDataColIdx; }


}
