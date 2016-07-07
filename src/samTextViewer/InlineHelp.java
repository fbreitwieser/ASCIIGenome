package samTextViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;

public class InlineHelp {

	public static Map<String, String> populateParams(){
		
		Map<String, String> params= new HashMap<String, String>();
		
		List<String> paramList= new ArrayList<String>();
		paramList.add("q");
		paramList.add("h");
		paramList.add("f");
		paramList.add("b");
		paramList.add("ff");
		paramList.add("bb");
		paramList.add("zi");
		paramList.add("zo");
		paramList.add("goto");
		paramList.add("from");
		paramList.add("+");
		paramList.add("-");
		paramList.add("p");
		paramList.add("n");
		paramList.add("next");
		paramList.add("next_start");
		paramList.add("find_first");
		paramList.add("find_all");
		paramList.add("seqRegex");
		paramList.add("visible");
		paramList.add("trackHeight");
		paramList.add("trackColour");
		paramList.add("ylim");
		paramList.add("dataCol");
		paramList.add("print");
		paramList.add("printFull");
		paramList.add("showGenome");
		paramList.add("addTracks");
		paramList.add("orderTracks");
		paramList.add("history");
		paramList.add("rpm");
		paramList.add("-f");
		paramList.add("-F");
		paramList.add("mapq");
		paramList.add("maxLines");
		for(String x : paramList){
			if(params.containsKey(x)){
				try { // Check there are no duplicate params
					throw new Exception();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			params.put(x, x);
		}
		return(params);
	}
		
	public static String getHelp() {

		StrSubstitutor sub = new StrSubstitutor(populateParams());

		String inline= "\n    N a v i g a t i o n   \n\n"
+ "${f} / ${b} \n"
+ "      Small step forward/backward 1/10 window\n"
+ "${ff} / ${bb}\n"
+ "${zi} / ${zo} [x]\n"
+ "      Zoom in / zoom out x times (default x= 1). Each zoom halves or doubles the window size\n"
+ "      Large step forward/backward 1/2 window\n"
+ "${goto} chrom:from-to\n"
+ "      Go to given region. E.g. \"goto chr1:1-1000\" or chr1:10 or chr1. goto keyword can be replaced with ':' (like goto in vim)\n"
+ "${from} [to]\n"
+ "      Go to position <from> or to region \"from to\" on current chromosome. E.g. 10 or \"10 1000\" or \"10-1000\"\n" 
+ "${+}/${-}<int>[k,m]\n"
+ "      Move forward/backward by <int> bases. Suffixes k (kilo) and M (mega) allowed. E.g. -2m or +10k\n"
+ "${p} / ${n}\n"
+ "      Go to previous/next visited position\n"
+ "${next} / ${next_start} [trackId]\n"
+ "      Move to the next feature in trackId on *current* chromosome\n"
+ "      'next' centers the window on the found feature while 'next_start' sets the window at the start of the feature.\n"

+ "\n    F i n d  \n\n"
+ "      Memo: To match regex case-insensitive prepend (?i) to pattern. E.g. (?i)actb will match ACTB\n\n"
+ "${find_first} <regex> [trackId]\n"
+ "      Find the first (next) record in trackId containing regex. Use single quotes for strings with spaces.\n"
+ "${find_all} <regex> [trackId]\n"
+ "      Find all records on chromosome containing regex. The search stops at the first chromosome\n"
+ "      returning hits starting with the current one. Useful to get all gtf records of a gene\n"
+ "${seqRegex} <regex>\n"
+ "      Find regex in reference sequence and show matches as and additional track.\n"
+ "      Useful to show restriction enzyme sites, CpGs etc.\n"

+ "\n    D i s p l a y  \n\n"
+ "${visible} [show regex] [hide regex] [track regex]\n"
+ "      In annotation tracks, only include rows containing [show regex] and exclude [hide regex].\n"
+ "      Apply to tracks containing  [track regex]. With no optional arguments reset to default: \"'.*' '^$' '.*'\"\n"
+ "      Use '.*' to match everything and '^$' to hide nothing. E.g. \"visible exon CDS gtf\"\n"       
+ "${trackHeight} <int> [track regex]\n"
+ "      Set track height to int lines for all tracks containing regex. Default regex: '.*'\n"
+ "${trackColour} <colour> [track regex]\n"
+ "      Set title colour for tracks containing regex. All colours except white and black\n"
+ "      accept the prefix 'light_'. Available colours:\n"
+ "      red green yellow blue magenta cyan grey white black\n"
+ "      E.g. trackColour light_blue ts.*gtf\n"
+ "${ylim} <min> <max> [track regex]\n"
+ "      Set limits of y axis for all track IDs containing regex. Use na to autoscale to min and/or max.\n"
+ "      E.g. ylim 0 na. If regex is omitted all tracks will be captured. Default: \"ylim na na .*\"\n"
+ "${dataCol} <idx> [regex]\n"
+ "      Select data column for all bedgraph tracks containing regex. <idx>: 1-based column index.\n"
+ "${print}     [track regex] \n"
+ "${printFull} [track regex] \n"
+ "      Print the lines of the annotation tracks containing  [track regex]. print clips lines to\n"
+ "      fit the screen. printFull wraps long lines. With no arguments all tracks are printed.\n"
+ "${showGenome}\n"
+ "      Print the genome file\n"
+ "${addTracks} [file or url]...\n"
+ "      Add tracks\n" 
+ "${orderTracks} [track#1] [track#2]...\n"
+ "      Reorder tracks\n" 
+ "${history}\n"
+ "      Show visited positions\n"

+ "\n    A l i g n m e n t s \n\n"
+ "${rpm} [track regex]\n"
+ "      Toggle display of read coverage from raw count to reads per million\n"
+ "      for alignment files containing [track regex]\n"
+ "${-f} INT \n"
+ "${-F} INT \n"
+ "      Include (-f) and exclude (-F) reads with INT bits set\n"
+ "${mapq} INT\n"
+ "      Include reads with mapq >= INT\n"
+ "${maxLines} INT\n"
+ "      Maximum number of lines to print for alignment tracks\n"
+ "\n${q} "
+ "      Quit\n" 
+ "${h} "
+ "      Show this help. See also " + ArgParse.WEB_ADDRESS + "\n";
							
		String fmtHelp= sub.replace(inline);
				
		return fmtHelp;
	}

	
	
}