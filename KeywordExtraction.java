import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * This class is used to extract keywords
 * @author Karthik
 * @since 14-October-2013
 */
public class KeywordExtraction {
	
	//Declare all the class variables
	private static String CSV_TRAINFILENAME = null;
	private static String CSV_TESTFILENAME = null;
	private static HashSet<String> stopWords = null;
	private static Map<String, Integer> listOfWords = new TreeMap<String, Integer>();
	private static int headerFlag = 0;
	
	/**
	 * This is the main method where the execution of the program starts
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		if(args.length == 0 || args.length < 2){
			System.out.println("Please enter the Training and Test file path");
		}
		else{
			CSV_TRAINFILENAME = args[0];
			CSV_TESTFILENAME = args[1];
			//Manually set up all the stop words in a Hash set by calling the below function
			createStopWords();
			//Call following method to get the model from the train file
			readTrainingFile();
			//Apply the model got from the train to the test using below method
			readTestFile();
		}

	}
	
	/**
	 * If there is 10 CSV columns. Then 10 processors has to be defined. This is for Training data set.
	 * @author Karthik
	 * @since 18-October-2013
	 * @return CellProcessor[]
	 */
	private static CellProcessor[] getTrainProcessors() {
		
		final CellProcessor[] processors = new CellProcessor[] { 
			new NotNull(), // Id
			new NotNull(), // Title
			new NotNull(), //Body
			new NotNull(), //Tags - Most Important
		};
		
		return processors;
	}
	
	/**
	 * If there is 10 CSV columns. Then 10 processors has to be defined. This is for test data set.
	 * @author Karthik
	 * @since 18-October-2013
	 * @return CellProcessor[]
	 */
   private static CellProcessor[] getTestProcessors() {
		
		final CellProcessor[] processors = new CellProcessor[] { 
			new NotNull(), // Id
			new NotNull(), // Title
			new NotNull(), //Body
		};
		
		return processors;
	}
	
   /**
	 * This function is used to separate the Id, Title, Body and Tags from the training set. Obtain a model from the training set
	 * @author Karthik
	 * @since 15-October-2013
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	private static void readTrainingFile() throws Exception {
		
		ICsvListReader listReader = null;
		try {
			listReader = new CsvListReader(new FileReader(CSV_TRAINFILENAME), CsvPreference.STANDARD_PREFERENCE);
			
			//skip the header since this is not required
			listReader.getHeader(true);
			
			//Get all the processors for the training file
			final CellProcessor[] processors = getTrainProcessors();
			
			List<Object> trainList;

			System.out.println("Forming a model based on the training set...");
			//Iterate through each record and form a model and store all tags and remove all the duplicates from the list
			while( (trainList = listReader.read(processors)) != null ) {
				//System.out.println(String.format("Id=%s, Title=%s, Body=%s", trainList.get(0), trainList.get(1), trainList.get(2)));
				String tags = trainList.get(3).toString();
				
				Scanner sc = new Scanner(tags);
				while(sc.hasNext()){
					String word = sc.next();
					int countWord = 0;
					if(!listOfWords.containsKey(word)){
						listOfWords.put(word, 1);
					}
					else{
						countWord = listOfWords.get(word)+1;
						listOfWords.remove(word);
						listOfWords.put(word, countWord);
					}
				}
			}
			System.out.println("Finished modelling training set. Apply the same to training set...");
		}
		finally {
			if( listReader != null ) {
				listReader.close();
			}
		}
	}
   
	/**
	 * This method is used to read the test file and applies the model which was got from train file
	 * @throws Exception
	 * @author Karthik
	 * @since 20-October-2013
	 */
	private static void readTestFile() throws Exception{
		ICsvListReader listReader = null;
		String titleOfFile = null;
		String bodyOfFile = null;
		List<Object> questionList;
		int Id = 0;
		try {
			listReader = new CsvListReader(new FileReader(CSV_TESTFILENAME), CsvPreference.STANDARD_PREFERENCE);
			
			//skip the header since this is not required
			listReader.getHeader(true);
			
			//Get all the processors for the test file
			final CellProcessor[] getTestProcessors = getTestProcessors();
			
			System.out.println("Running the model on test file...");
			while( (questionList = listReader.read(getTestProcessors)) != null ) {
				//Get the Id of the question
				Id = Integer.parseInt(questionList.get(0).toString());
				
				//Read the title of the test file and remove the stop words from that particular record
				if(questionList.get(1).toString() != null){
					titleOfFile = questionList.get(1).toString().toLowerCase();
					//Remove stop words from title
					titleOfFile = removeStopWords(titleOfFile);
				}
				
				//Read the body of the test file and remove the stop words from that particular record
				if(questionList.get(2).toString() != null){
					bodyOfFile = questionList.get(2).toString().toLowerCase();
					//Remove stop words from body
					bodyOfFile = removeStopWords(bodyOfFile);
				}
				
				//Calculate the probability of the keywords in both title and body of the records
				Map<String, Integer> map = probabilityCalc(Id, titleOfFile, bodyOfFile);
				
				//Compare against the model of the training set
				map = compareKeywords(Id, map);
				
				//Sort the given result according to term frequency
				map = sortByValues(map);
				
				//Finally, write the Id and tags to a file
				writeToFile(Id, map);
			}
			System.out.println("Finished keyword extraction of training set...");
			System.out.println("Result.txt file created with appropriate tags...");
		}
		finally {
			if( listReader != null ) {
				listReader.close();
			}
		}
	}
	
	/**
	 * This function is used to write the final output to the file
	 * @author Karthik
	 * @since 04-November-2013
	 * @param Id
	 * @param map
	 */
	private static void writeToFile(int Id, Map<String, Integer> map){
		try{
			if(map!= null){
				
				//Create a file name called result.txt
				File file = new File("result.txt");
				
				//If the file does not exist then create a new file
				if(!file.exists()){
					file.createNewFile();
				}
				
				//This condition is for writing Id,tags on the top of the file
				if(headerFlag == 0){
					FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write("\"Id\",\"Tags\"");
					bw.newLine();
					bw.close();
					headerFlag = -1;
				}
	
				//This part of the functions writes all the tags along with their Id to the file called result.txt
				FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(Id+",");
				int i = 0;
				bw.write("\"");
				for (Map.Entry<String, Integer> entry : map.entrySet()) {
					if(i == 5){
						break;
					}
					else if(i == 4){
						bw.write(entry.getKey());
					}
					else{
						bw.write(entry.getKey()+" ");
					}
					i++;
				}
				bw.write("\"");
				bw.newLine();
				bw.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This function is used to compare the probability of classes of keywords with test file obtained from training file
	 * @author Karthik
	 * @since 29-October-2013
	 * @param Id
	 * @param map
	 * @return Map<String, Integer> containing all the possible keywords of the test file
	 */
	private static Map<String, Integer> compareKeywords(int Id, Map<String, Integer> map){
		Map<String, Integer> cMap = new TreeMap<String, Integer>();
		try{
			if(!map.isEmpty()){
				for (Map.Entry<String, Integer> entry : map.entrySet()) {
					if(listOfWords.containsKey(entry.getKey())){
						int count = entry.getValue()+listOfWords.get(entry.getKey());
						cMap.put(entry.getKey(), count);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return cMap;
	}
	
	/**
	 * This function is used to remove all the stop words and all the unwanted symbols and text from the given string
	 * @author Karthik
	 * @since 20-October-2013
	 * @param str
	 * @return String which is free from stop words and unwanted symbols
	 */
	@SuppressWarnings("resource")
	private static String removeStopWords(String str){
		StringBuilder tempString = new StringBuilder();
		String result = null;
		try{
			str = str.replaceAll("[^\\w\\s-]", " "); 
			Scanner sc = new Scanner(str);
			while(sc.hasNext()){
				String word = sc.next();
				
				if(!stopWords.contains(word.toLowerCase())){
					tempString.append(word+" ");
				}
			}
			result = tempString.toString();
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * This function is used to calculate the probability of a word being a keyword in particular record
	 * @author Karthik
	 * @since 25-October-2013
	 * @param title
	 * @param body
	 * @return
	 */
	@SuppressWarnings({ "resource" })
	private static Map<String, Integer> probabilityCalc(int Id, String title, String body){
		Map<String, Integer> hm = new HashMap<String, Integer>();
		try{
			Scanner sc = new Scanner(title);
			while(sc.hasNext()){
				String word = sc.next();
				int countWord = 0;
				if(!hm.containsKey(word)){
					hm.put(word, 1);
				}
				else{
					countWord = hm.get(word)+1;
					hm.remove(word);
					hm.put(word, countWord);
				}
			}
			
			Scanner sc1 = new Scanner(body);
			while(sc1.hasNext()){
				String word = sc1.next();
				int countWord = 0;
				if(!hm.containsKey(word)){
					hm.put(word, 1);
				}
				else{
					countWord = hm.get(word)+1;
					hm.remove(word);
					hm.put(word, countWord);
				}
			}
			
			hm = sortByValues(hm);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return hm;
	}

	/**
	 * This function is used to sort a Map based on values
	 * @author Karthik
	 * @since 25-October-2013
	 * @param map
	 * @return Map containing sorted values
	 */
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
	    Comparator<K> valueComparator =  new Comparator<K>() {
	        public int compare(K k1, K k2) {
	            int compare = map.get(k2).compareTo(map.get(k1));
	            if (compare == 0) return 1;
	            else return compare;
	        }
	    };
	    Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    sortedByValues.putAll(map);
	    return sortedByValues;
	}
	
	/**
	 * This function is used to add stop words to a hashset which does not contain duplicates values
	 * @author Karthik
	 * @since 18-October-2013
	 * @param word
	 */
	private static void addStopWords(String word){
		if(word.trim().length() > 0 && !word.isEmpty()){
			stopWords.add(word.trim().toLowerCase());
		}
	}
	
	/**
	 * This function is used to create a Hash Set of stop words
	 * @author Karthik
	 * @since 18-October-2013
	 */
	private static void createStopWords(){
		stopWords = new HashSet<String>();
		
		addStopWords("a");
		addStopWords("able");
		addStopWords("about");
		addStopWords("above");
		addStopWords("abst");
		addStopWords("accordance");
		addStopWords("according");
		addStopWords("accordingly");
		addStopWords("across");
		addStopWords("act");
		addStopWords("actual");
		addStopWords("actually");
		addStopWords("add");
		addStopWords("added");
		addStopWords("adj");
		addStopWords("affect");
		addStopWords("affected");
		addStopWords("affecting");
		addStopWords("affects");
		addStopWords("after");
		addStopWords("afterwards");
		addStopWords("again");
		addStopWords("against");
		addStopWords("ah");
		addStopWords("all");
		addStopWords("almost");
		addStopWords("alone");
		addStopWords("along");
		addStopWords("already");
		addStopWords("also");
		addStopWords("although");
		addStopWords("always");
		addStopWords("am");
		addStopWords("among");
		addStopWords("amongst");
		addStopWords("an");
		addStopWords("and");
		addStopWords("announce");
		addStopWords("another");
		addStopWords("any");
		addStopWords("anybody");
		addStopWords("anyhow");
		addStopWords("anymore");
		addStopWords("anyone");
		addStopWords("anything");
		addStopWords("anyway");
		addStopWords("anyways");
		addStopWords("anywhere");
		addStopWords("apparently");
		addStopWords("approximately");
		addStopWords("are");
		addStopWords("aren");
		addStopWords("arent");
		addStopWords("aren't");
		addStopWords("arise");
		addStopWords("around");
		addStopWords("as");
		addStopWords("aside");
		addStopWords("ask");
		addStopWords("asking");
		addStopWords("at");
		addStopWords("auth");
		addStopWords("avail");
		addStopWords("available");
		addStopWords("aw");
		addStopWords("away");
		addStopWords("awfully");
		addStopWords("b");
		addStopWords("back");
		addStopWords("be");
		addStopWords("became");
		addStopWords("because");
		addStopWords("become");
		addStopWords("becomes");
		addStopWords("becoming");
		addStopWords("been");
		addStopWords("before");
		addStopWords("beforehand");
		addStopWords("begin");
		addStopWords("beginning");
		addStopWords("beginnings");
		addStopWords("begins");
		addStopWords("behind");
		addStopWords("being");
		addStopWords("believe");
		addStopWords("below");
		addStopWords("beside");
		addStopWords("besides");
		addStopWords("between");
		addStopWords("beyond");
		addStopWords("biol");
		addStopWords("both");
		addStopWords("brief");
		addStopWords("briefly");
		addStopWords("but");
		addStopWords("by");
		addStopWords("ca");
		addStopWords("came");
		addStopWords("can");
		addStopWords("cannot");
		addStopWords("cant");
		addStopWords("can't");
		addStopWords("cause");
		addStopWords("causes");
		addStopWords("certain");
		addStopWords("certainly");
		addStopWords("co");
		addStopWords("com");
		addStopWords("come");
		addStopWords("comes");
		addStopWords("contain");
		addStopWords("containing");
		addStopWords("contains");
		addStopWords("could");
		addStopWords("couldnt");
		addStopWords("d");
		addStopWords("date");
		addStopWords("did");
		addStopWords("didnt");
		addStopWords("didn't");
		addStopWords("different");
		addStopWords("do");
		addStopWords("does");
		addStopWords("doesnt");
		addStopWords("doesn't");
		addStopWords("doing");
		addStopWords("done");
		addStopWords("dont");
		addStopWords("don't");
		addStopWords("down");
		addStopWords("downwards");
		addStopWords("due");
		addStopWords("during");
		addStopWords("e");
		addStopWords("each");
		addStopWords("ed");
		addStopWords("edu");
		addStopWords("effect");
		addStopWords("eg");
		addStopWords("eight");
		addStopWords("eighty");
		addStopWords("either");
		addStopWords("else");
		addStopWords("elsewhere");
		addStopWords("end");
		addStopWords("ending");
		addStopWords("enough");
		addStopWords("especially");
		addStopWords("et");
		addStopWords("et-al");
		addStopWords("etc");
		addStopWords("even");
		addStopWords("ever");
		addStopWords("every");
		addStopWords("everybody");
		addStopWords("everyone");
		addStopWords("everything");
		addStopWords("everywhere");
		addStopWords("ex");
		addStopWords("except");
		addStopWords("f");
		addStopWords("far");
		addStopWords("few");
		addStopWords("ff");
		addStopWords("fifth");
		addStopWords("find");
		addStopWords("first");
		addStopWords("five");
		addStopWords("fix");
		addStopWords("followed");
		addStopWords("following");
		addStopWords("follows");
		addStopWords("for");
		addStopWords("former");
		addStopWords("formerly");
		addStopWords("forth");
		addStopWords("found");
		addStopWords("four");
		addStopWords("from");
		addStopWords("further");
		addStopWords("furthermore");
		addStopWords("g");
		addStopWords("gave");
		addStopWords("get");
		addStopWords("gets");
		addStopWords("getting");
		addStopWords("give");
		addStopWords("given");
		addStopWords("gives");
		addStopWords("giving");
		addStopWords("go");
		addStopWords("goes");
		addStopWords("gone");
		addStopWords("got");
		addStopWords("gotten");
		addStopWords("h");
		addStopWords("had");
		addStopWords("happens");
		addStopWords("hardly");
		addStopWords("has");
		addStopWords("hasnt");
		addStopWords("hasn't");
		addStopWords("have");
		addStopWords("haven't");
		addStopWords("havent");
		addStopWords("having");
		addStopWords("he");
		addStopWords("hed");
		addStopWords("hence");
		addStopWords("her");
		addStopWords("here");
		addStopWords("hereafter");
		addStopWords("hereby");
		addStopWords("herein");
		addStopWords("heres");
		addStopWords("hereupon");
		addStopWords("hers");
		addStopWords("herself");
		addStopWords("hes");
		addStopWords("hi");
		addStopWords("hid");
		addStopWords("hide");
		addStopWords("him");
		addStopWords("himself");
		addStopWords("his");
		addStopWords("hither");
		addStopWords("home");
		addStopWords("how");
		addStopWords("howbeit");
		addStopWords("however");
		addStopWords("hundred");
		addStopWords("i");
		addStopWords("id");
		addStopWords("ie");
		addStopWords("if");
		addStopWords("i'll");
		addStopWords("im");
		addStopWords("i'm");
		addStopWords("immediate");
		addStopWords("immediately");
		addStopWords("importance");
		addStopWords("important");
		addStopWords("in");
		addStopWords("inc");
		addStopWords("indeed");
		addStopWords("index");
		addStopWords("information");
		addStopWords("instead");
		addStopWords("into");
		addStopWords("invention");
		addStopWords("inward");
		addStopWords("is");
		addStopWords("isn't");
		addStopWords("it");
		addStopWords("itd");
		addStopWords("it'll");
		addStopWords("itll");
		addStopWords("its");
		addStopWords("itself");
		addStopWords("i've");
		addStopWords("ive");
		addStopWords("j");
		addStopWords("just");
		addStopWords("k");
		addStopWords("keep");
		addStopWords("keeps");
		addStopWords("kept");
		addStopWords("kg");
		addStopWords("km");
		addStopWords("know");
		addStopWords("known");
		addStopWords("knows");
		addStopWords("l");
		addStopWords("largely");
		addStopWords("last");
		addStopWords("lately");
		addStopWords("later");
		addStopWords("latter");
		addStopWords("latterly");
		addStopWords("least");
		addStopWords("less");
		addStopWords("lest");
		addStopWords("let");
		addStopWords("lets");
		addStopWords("like");
		addStopWords("liked");
		addStopWords("likely");
		addStopWords("line");
		addStopWords("little");
		addStopWords("'ll");
		addStopWords("look");
		addStopWords("looking");
		addStopWords("looks");
		addStopWords("ltd");
		addStopWords("m");
		addStopWords("made");
		addStopWords("mainly");
		addStopWords("make");
		addStopWords("makes");
		addStopWords("many");
		addStopWords("may");
		addStopWords("maybe");
		addStopWords("me");
		addStopWords("mean");
		addStopWords("means");
		addStopWords("meantime");
		addStopWords("meanwhile");
		addStopWords("merely");
		addStopWords("mg");
		addStopWords("might");
		addStopWords("million");
		addStopWords("miss");
		addStopWords("ml");
		addStopWords("more");
		addStopWords("moreover");
		addStopWords("most");
		addStopWords("mostly");
		addStopWords("mr");
		addStopWords("mrs");
		addStopWords("much");
		addStopWords("mug");
		addStopWords("must");
		addStopWords("my");
		addStopWords("myself");
		addStopWords("n");
		addStopWords("na");
		addStopWords("name");
		addStopWords("namely");
		addStopWords("nay");
		addStopWords("nd");
		addStopWords("near");
		addStopWords("nearly");
		addStopWords("necessarily");
		addStopWords("necessary");
		addStopWords("need");
		addStopWords("needs");
		addStopWords("neither");
		addStopWords("never");
		addStopWords("nevertheless");
		addStopWords("new");
		addStopWords("next");
		addStopWords("nine");
		addStopWords("ninety");
		addStopWords("no");
		addStopWords("nobody");
		addStopWords("non");
		addStopWords("none");
		addStopWords("nonetheless");
		addStopWords("noone");
		addStopWords("nor");
		addStopWords("normally");
		addStopWords("nos");
		addStopWords("not");
		addStopWords("noted");
		addStopWords("nothing");
		addStopWords("now");
		addStopWords("nowhere");
		addStopWords("o");
		addStopWords("obtain");
		addStopWords("obtained");
		addStopWords("obviously");
		addStopWords("of");
		addStopWords("off");
		addStopWords("often");
		addStopWords("oh");
		addStopWords("ok");
		addStopWords("okay");
		addStopWords("old");
		addStopWords("omitted");
		addStopWords("on");
		addStopWords("once");
		addStopWords("one");
		addStopWords("ones");
		addStopWords("only");
		addStopWords("onto");
		addStopWords("or");
		addStopWords("ord");
		addStopWords("other");
		addStopWords("others");
		addStopWords("otherwise");
		addStopWords("ought");
		addStopWords("our");
		addStopWords("ours");
		addStopWords("ourselves");
		addStopWords("out");
		addStopWords("outside");
		addStopWords("over");
		addStopWords("overall");
		addStopWords("owing");
		addStopWords("own");
		addStopWords("p");
		addStopWords("page");
		addStopWords("pages");
		addStopWords("part");
		addStopWords("particular");
		addStopWords("particularly");
		addStopWords("past");
		addStopWords("per");
		addStopWords("perhaps");
		addStopWords("placed");
		addStopWords("please");
		addStopWords("plus");
		addStopWords("poorly");
		addStopWords("possible");
		addStopWords("possibly");
		addStopWords("potentially");
		addStopWords("pp");
		addStopWords("predominantly");
		addStopWords("present");
		addStopWords("previously");
		addStopWords("primarily");
		addStopWords("probably");
		addStopWords("promptly");
		addStopWords("proud");
		addStopWords("provides");
		addStopWords("put");
		addStopWords("q");
		addStopWords("que");
		addStopWords("quickly");
		addStopWords("quite");
		addStopWords("qv");
		addStopWords("ran");
		addStopWords("rather");
		addStopWords("rd");
		addStopWords("re");
		addStopWords("ready");
		addStopWords("readily");
		addStopWords("really");
		addStopWords("recent");
		addStopWords("recently");
		addStopWords("ref");
		addStopWords("refs");
		addStopWords("regarding");
		addStopWords("regardless");
		addStopWords("regards");
		addStopWords("related");
		addStopWords("relatively");
		addStopWords("research");
		addStopWords("respectively");
		addStopWords("resulted");
		addStopWords("resulting");
		addStopWords("results");
		addStopWords("right");
		addStopWords("run");
		addStopWords("s");
		addStopWords("said");
		addStopWords("same");
		addStopWords("saw");
		addStopWords("say");
		addStopWords("saying");
		addStopWords("says");
		addStopWords("sec");
		addStopWords("section");
		addStopWords("see");
		addStopWords("seeing");
		addStopWords("seem");
		addStopWords("seemed");
		addStopWords("seeming");
		addStopWords("seems");
		addStopWords("seen");
		addStopWords("self");
		addStopWords("selves");
		addStopWords("sent");
		addStopWords("seven");
		addStopWords("several");
		addStopWords("shall");
		addStopWords("she");
		addStopWords("shed");
		addStopWords("she'll");
		addStopWords("shes");
		addStopWords("should");
		addStopWords("shouldnt");
		addStopWords("shouldn't");
		addStopWords("show");
		addStopWords("showed");
		addStopWords("shown");
		addStopWords("showns");
		addStopWords("shows");
		addStopWords("significant");
		addStopWords("significantly");
		addStopWords("similar");
		addStopWords("similarly");
		addStopWords("since");
		addStopWords("six");
		addStopWords("slightly");
		addStopWords("so");
		addStopWords("some");
		addStopWords("somebody");
		addStopWords("somehow");
		addStopWords("someone");
		addStopWords("somethan");
		addStopWords("something");
		addStopWords("sometime");
		addStopWords("sometimes");
		addStopWords("somewhat");
		addStopWords("somewhere");
		addStopWords("soon");
		addStopWords("sorry");
		addStopWords("specifically");
		addStopWords("specified");
		addStopWords("specify");
		addStopWords("specifying");
		addStopWords("still");
		addStopWords("stop");
		addStopWords("strongly");
		addStopWords("sub");
		addStopWords("substantially");
		addStopWords("successfully");
		addStopWords("such");
		addStopWords("sufficiently");
		addStopWords("suggest");
		addStopWords("sup");
		addStopWords("sure");
		addStopWords("t");
		addStopWords("take");
		addStopWords("taken");
		addStopWords("taking");
		addStopWords("tell");
		addStopWords("tends");
		addStopWords("th");
		addStopWords("than");
		addStopWords("thank");
		addStopWords("thanks");
		addStopWords("thanx");
		addStopWords("that");
		addStopWords("that'll");
		addStopWords("thats");
		addStopWords("that've");
		addStopWords("the");
		addStopWords("their");
		addStopWords("theirs");
		addStopWords("them");
		addStopWords("themselves");
		addStopWords("then");
		addStopWords("thence");
		addStopWords("there");
		addStopWords("thereafter");
		addStopWords("thereby");
		addStopWords("thered");
		addStopWords("therefore");
		addStopWords("therein");
		addStopWords("there'll");
		addStopWords("thereof");
		addStopWords("therere");
		addStopWords("theres");
		addStopWords("thereto");
		addStopWords("thereupon");
		addStopWords("there've");
		addStopWords("these");
		addStopWords("they");
		addStopWords("theyd");
		addStopWords("they'll");
		addStopWords("theyre");
		addStopWords("they've");
		addStopWords("think");
		addStopWords("this");
		addStopWords("those");
		addStopWords("thou");
		addStopWords("though");
		addStopWords("thoughh");
		addStopWords("thousand");
		addStopWords("throug");
		addStopWords("through");
		addStopWords("throughout");
		addStopWords("thru");
		addStopWords("thus");
		addStopWords("til");
		addStopWords("tip");
		addStopWords("to");
		addStopWords("together");
		addStopWords("too");
		addStopWords("took");
		addStopWords("toward");
		addStopWords("towards");
		addStopWords("tried");
		addStopWords("tries");
		addStopWords("truly");
		addStopWords("try");
		addStopWords("trying");
		addStopWords("ts");
		addStopWords("twice");
		addStopWords("two");
		addStopWords("u");
		addStopWords("un");
		addStopWords("under");
		addStopWords("unfortunately");
		addStopWords("unless");
		addStopWords("unlike");
		addStopWords("unlikely");
		addStopWords("until");
		addStopWords("unto");
		addStopWords("up");
		addStopWords("upon");
		addStopWords("ups");
		addStopWords("us");
		addStopWords("use");
		addStopWords("used");
		addStopWords("useful");
		addStopWords("usefully");
		addStopWords("usefulness");
		addStopWords("uses");
		addStopWords("using");
		addStopWords("usually");
		addStopWords("v");
		addStopWords("value");
		addStopWords("various");
		addStopWords("'ve");
		addStopWords("very");
		addStopWords("via");
		addStopWords("viz");
		addStopWords("vol");
		addStopWords("vols");
		addStopWords("vs");
		addStopWords("w");
		addStopWords("want");
		addStopWords("wants");
		addStopWords("was");
		addStopWords("wasn't");
		addStopWords("way");
		addStopWords("we");
		addStopWords("wed");
		addStopWords("welcome");
		addStopWords("we'll");
		addStopWords("went");
		addStopWords("were");
		addStopWords("weren't");
		addStopWords("we've");
		addStopWords("what");
		addStopWords("whatever");
		addStopWords("what'll");
		addStopWords("whats");
		addStopWords("when");
		addStopWords("whence");
		addStopWords("whenever");
		addStopWords("where");
		addStopWords("whereafter");
		addStopWords("whereas");
		addStopWords("whereby");
		addStopWords("wherein");
		addStopWords("wheres");
		addStopWords("whereupon");
		addStopWords("wherever");
		addStopWords("whether");
		addStopWords("which");
		addStopWords("while");
		addStopWords("whim");
		addStopWords("whither");
		addStopWords("who");
		addStopWords("whod");
		addStopWords("whoever");
		addStopWords("whole");
		addStopWords("who'll");
		addStopWords("whom");
		addStopWords("whomever");
		addStopWords("whos");
		addStopWords("whose");
		addStopWords("why");
		addStopWords("widely");
		addStopWords("willing");
		addStopWords("wish");
		addStopWords("with");
		addStopWords("within");
		addStopWords("without");
		addStopWords("won't");
		addStopWords("words");
		addStopWords("world");
		addStopWords("would");
		addStopWords("wouldn't");
		addStopWords("write");
		addStopWords("www");
		addStopWords("x");
		addStopWords("y");
		addStopWords("yes");
		addStopWords("yet");
		addStopWords("you");
		addStopWords("youd");
		addStopWords("you'll");
		addStopWords("your");
		addStopWords("youre");
		addStopWords("yours");
		addStopWords("yourself");
		addStopWords("yourselves");
		addStopWords("you've");
		addStopWords("z");
		addStopWords("zero");
		
		//Newly Added
		addStopWords("small");
		addStopWords("contents");
		addStopWords("lt");
		addStopWords("gt");
		addStopWords("code");
		addStopWords("example");
		addStopWords("inside");
		addStopWords("site");
		addStopWords("div");
		addStopWords("pre");
		addStopWords("par");
		addStopWords("set");
		addStopWords("class");
		addStopWords("strong");
		addStopWords("happen");
		addStopWords("examine");
		addStopWords("user");
		addStopWords("super");
		addStopWords("method");
		addStopWords("ve");
		addStopWords("copy");
		addStopWords("cut");
		addStopWords("paste");
		addStopWords("remove");
		addStopWords("allow");
		addStopWords("company");
		addStopWords("span");
		addStopWords("li");
		addStopWords("ul");
		addStopWords("var");
		addStopWords("will");
		addStopWords("project");
		addStopWords("include");
		addStopWords("string");
		addStopWords("foo");
		addStopWords("iterate");
	}
	
}
