
import java.util.*;
import java.io.*;
import org.apache.commons.cli.*; 
// compile:	javac -cp ".:commons-cli-1.3.jar" Solution.java
// run:		java -cp ".:commons-cli-1.3.jar" Solution -c -g -p -r


public class Solution {
	private final String TSV_PATH_PREFIX = "./out";
	private final String TSV_USERS = TSV_PATH_PREFIX + "/users.tsv";
	private final String TSV_RECOMMENDATIONS = TSV_PATH_PREFIX + "/recommendations.tsv";
	private final String TSV_PET = TSV_PATH_PREFIX + "/pet_items.tsv";
	private final String TSV_HIGH_ROLLER = TSV_PATH_PREFIX + "/high_roller_items.tsv";
	private final String TSV_MALE = TSV_PATH_PREFIX + "/male_items.tsv";
	private final String TSV_FEMALE = TSV_PATH_PREFIX + "/female_items.tsv";
	private final String TSV_NOT_CA = TSV_PATH_PREFIX + "/not_available_in_california_items.tsv";
	
	private final String OUT_RECOMMENDATIONS = "./new_recommendations.tsv";
	
	private final long CHUNK_LINES = 10000;
	private final List<Recommendation> g_recommList = new ArrayList<Recommendation>();
	private final Map<Long, User> g_userMap = new HashMap<Long, User>();
	
	private final Set<String> g_petSet = new HashSet<String>();
	private final Set<String> g_highRollerSet = new HashSet<String>();
	private final Set<String> g_maleSet = new HashSet<String>();
	private final Set<String> g_femaleSet = new HashSet<String>();
	private final Set<String> g_notCASet = new HashSet<String>();
	
	private boolean gf_petItems = false;
	private boolean gf_highRoller = false;
	private boolean gf_gender = false;
	private boolean gf_notCA = false;
	
	// ============================================================================
	
	private void usage(Options opts) {
		String header = "Please provide at least one option\n\n";
		String footer = "\nPlease report issues at ys1488@nyu.edu, Shelley Su\n\n";

		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java Solution", header, opts, footer, true);
		System.exit(1);
	}
	
	private void parserArgs(String[] args) {		
		Options options = new Options();
		// add t option
		options.addOption(OptionBuilder.withLongOpt("gender")
	    	.withDescription("Remove gender-inappropriate items").create('g'));
	    options.addOption(OptionBuilder.withLongOpt("highroller")
	    	.withDescription("Remove high roller items").create('r'));
	    options.addOption(OptionBuilder.withLongOpt("pet")
	    	.withDescription("Remove pet items for non pet owners").create('p'));
	    options.addOption(OptionBuilder.withLongOpt("notCA")
	    	.withDescription("Remove \"not available in CA\" items for CA users").create('c'));
	    options.addOption(OptionBuilder.withLongOpt("help")
	    	.withDescription("Print this help message").create('h'));
	    
	    if (0 == args.length) {
			usage(options);
			return;			
		}
		
	    CommandLineParser parser = new PosixParser();
	    
		try {
			CommandLine cmd = parser.parse(options, args);
			
			/*
			int value = 0; // initialize to some meaningful default value
			if (cmd.hasOption("integer-option")) {
				value = ((Number)cmd.getParsedOptionValue("integer-option")).intValue();
			}
			System.out.println(value);
			*/
			
			if ( cmd.hasOption("g") ) {
				gf_gender = true;
			} 
		
			if ( cmd.hasOption("r") ) {
				gf_highRoller = true;
			} 
		
			if ( cmd.hasOption("p") ) {
				gf_petItems = true;
			} 
		
			if ( cmd.hasOption("c") ) {
				gf_notCA = true;
			}
		
		} catch ( ParseException exp ) {
			System.err.println("\nParsing of command line args failed.  Reason: " 
				+ exp.getMessage() + "\n");
			usage(options);
		}				
	}	

	// ===========================================================================
	
	private void readTSVs() {
		readUsers();
		readItems(TSV_PET, g_petSet);
		readItems(TSV_HIGH_ROLLER, g_highRollerSet);
		readItems(TSV_MALE, g_maleSet);
		readItems(TSV_FEMALE, g_femaleSet);
		readItems(TSV_NOT_CA, g_notCASet);
	}
	
	private List<List<String>> tsvRead(String path) {
		List<List<String>> ret = new ArrayList<List<String>>();
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader(path));
			String dataRow = br.readLine(); // Read first line.

			while (dataRow != null) {
		
				String[] arr = dataRow.split(",");
				List<String> list = new ArrayList<String>();
				
				for (String s : arr) {
					list.add(s);
				}
				
				ret.add(list);
			
				dataRow = br.readLine();
			}		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
			
		return ret;
	}
	
	private void readUsers() {
		List<List<String>> list = tsvRead(TSV_USERS);
		
		for (List<String> l : list) {
		
			long id = Long.valueOf( l.get(0) );
			int clv = Integer.valueOf( l.get(4) );
			User u = new User(id, l.get(1), l.get(2), l.get(3), clv);
			g_userMap.put(id, u);

		}
		
		list.clear();
	}
	
	private void readItems(String path, Set<String> set) {
		List<List<String>> list = tsvRead(path);
		
		for (List<String> l : list) {
			set.add(l.get(0));
		}
		
		list.clear();
	}
	
	// ======================================================================
	
	private void processRecommendations() { // process huge file partially
		long pos = 0, count = 0;		
		List<List<String>> list = new ArrayList<List<String>>();		    
        List<List<String>> blockList = new ArrayList<List<String>>();
        RandomAccessFile raf = null; 
        
        clearOldOutput();
        
        try {
			raf = new RandomAccessFile(TSV_RECOMMENDATIONS,"r");  
		    long totalLen = raf.length();  
		    //System.out.println("Total len: " + totalLen);
		    
		    String dataRow = "";
		    
		    if (0 != pos) {
		    	pos++;
		    	raf.seek(pos);
		    }
		    
		    while ( pos < totalLen ) {
		    
		    	byte b = raf.readByte();
		    	if ( '\n' == b ) {
		    		//System.out.println(dataRow);
		    		
		    		String[] arr = dataRow.split(",");
					List<String> l = new ArrayList<String>();
				
					for (String s : arr) {
						l.add(s);
					}
				
					blockList.add(l);
		    		
		    		
		    		dataRow = "";
		    		count++;
		    		
		    		if ( CHUNK_LINES == count ) {
		    			//System.out.println("raf pos: " + pos);
		    			processRecommendationsBlock(blockList);
		    			
		    			count = 0;
		    			blockList.clear();	    					
						g_recommList.clear();
		    			//break; // for test
		    		}
		    	} else {
					dataRow += (char)b;
				}
		    	
		    	pos++;
		    }
		    
		    if ( 0 != count && CHUNK_LINES > count ) { // last chunk	
    			//System.out.println("last raf pos: " + pos);
    			processRecommendationsBlock(blockList);
		    } 
		    
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void clearOldOutput() {
		File file = new File(OUT_RECOMMENDATIONS);
		
		if(file.exists() && file.isFile()) { 
			file.delete();
		}
	}
	
	private void processRecommendationsBlock(List<List<String>> list) {
		for (List<String> l : list) {
		
			long id = Long.valueOf( l.get(0) );
			l.remove(0);
			
			Recommendation r = new Recommendation(id, l);
			g_recommList.add(r);
		}
		
		applyFilters();
		appendOutput();
	}
	
	private void appendOutput() {
		// use g_recommList
		final String COMMA_DELIMITER = ",";
		final String NEW_LINE_SEPARATOR = "\n";
     
		FileWriter writer = null;

        try {
            writer = new FileWriter(OUT_RECOMMENDATIONS, true); // "true" means append

            for (Recommendation r : g_recommList) {
				writer.append( String.valueOf(r.user_id) );
								
				for (String s : r.items) {
					writer.append(COMMA_DELIMITER);
					writer.append(s);
				}
				
				writer.append(NEW_LINE_SEPARATOR);
				
			}
			
			writer.flush();
			writer.close();
			
        } catch(IOException e) {
     		e.printStackTrace();
		}
	}
	
	// ===========================================================================
		
	private void applyFilters() {
	
		for (Recommendation r : g_recommList) {
			User u = g_userMap.get(r.user_id);
			List<String> list = r.items;
			
			int i = 0;
			while ( i < list.size() ) {
				String item = list.get(i);
							
				if ( true == gf_gender 
					&& ( hasInappropriateItem(u.gender, "f", g_maleSet, item) == true 
					|| hasInappropriateItem(u.gender, "m", g_femaleSet, item) == true ) ) {
					
					r.items.remove(i);
					continue;
					
				} else if ( true == gf_highRoller 
					&& hasSmallerValue(u.estimated_clv, 100, g_highRollerSet, item) == true ) {
				
					r.items.remove(i);
					continue;
					
				} else if ( true == gf_notCA 
					&& hasInappropriateItem(u.state, "CA", g_notCASet, item) == true ) {
					
					r.items.remove(i);
					continue;
					
				} else if ( true == gf_petItems 
					&& hasInappropriateItem(u.state, "f", g_petSet, item) == true ) {
					
					r.items.remove(i);
					continue;
					
				} else {
					i++;
				} // if
				
			} // while
		} // for
						
	}
	
	private boolean hasInappropriateItem(String u_field, String val, Set<String> set, String item) {
		
		if ( u_field.equals("") ) {
			return false;
		}
		
		if ( u_field.equals(val) && set.contains(item) ) {
			return true;
		}
		
		return false;
	}
	
	private boolean hasSmallerValue(int u_field, int val, Set<String> set, String item) {

		if ( u_field < val && set.contains(item) ) {
			return true;
		}
		
		return false;
	}
	
	// =============================================================================

	public static void main(String[] args) {
		Solution s = new Solution();
		s.parserArgs(args);
		s.readTSVs();
		s.processRecommendations();
	}
}
