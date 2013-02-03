package clustering;

import java.io.*;
import java.util.Map.Entry;
import java.util.*;
/**
 *
 * @author Walkman
 */
public class Holder {
     private static int FREE_DOC_ID;
     private ArrayList<String> docIdList;
     private HashMap<String, HashMap<Integer, Integer>> index;  // <term, <docId, frequency>>
     private HashMap<String, HashMap<Integer, Double>> tf_Idf;  // <term, <docId, tfIdf>>
     private HashMap<Integer, Vector<Double>> docVec;           // <docId, vector>
     private HashMap<Integer, Double> vecLenght;                // <docId, vector lenght>
     private HashMap<Integer, Double> cosineSimilarities;       // <docID, cosineSimilarity with query>
     private ArrayList<Integer> leaders;
     private HashMap<Integer, ArrayList<Integer>> followers;    // <docId, list of leaders>
     private static int B1;  
     private static int B2; 
     private Vector<Double> queryVector;
     private String [] queryWords;
     
    public Holder(){
        queryVector = new Vector<>();
        FREE_DOC_ID = 0;
        B1 = 1;
        B2 = 1;
        index = new HashMap<>();
        tf_Idf = new HashMap<>();
        docVec = new HashMap<>();
        vecLenght = new HashMap<>();
        cosineSimilarities = new HashMap<>();
        docIdList = new ArrayList<>();
        leaders = new ArrayList<>(); 
        followers = new HashMap<>();
    
    }
    /**
     * Building invert index
     * @param folder Folder with documents
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public void buildIndexes(final File folder) throws FileNotFoundException,
    UnsupportedEncodingException, IOException, ClassNotFoundException {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                buildIndexes(fileEntry);
            } else {
                docIdList.add(fileEntry.getName());
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(
                        fileEntry.getAbsoluteFile()), "UTF-8"));
                String content = new Scanner(in).useDelimiter("\\Z").next();
                String body = content.substring(content.indexOf("<body>")+6,
                        content.indexOf("</body>"));
                body = body.replaceAll("</v><v>", " ");
                body = body.replaceAll("\\<.*?\\>", "");
                body = body.replaceAll("\n", " ");
                body = body.replaceAll("[\\W]", " ");

                String[] line = body.split(" ");
                for (int i = 0; i < line.length; i++) {
                    addTerm(line[i].toLowerCase(), FREE_DOC_ID);
                }
                FREE_DOC_ID++;
            }
        }
        calculateTfIdf();
        createVectors();
        clustering();
    }
      
    /**
     * Adding term to index
     * @param term Term
     * @param docId Document id
     */
    private void addTerm(String term, int docId) {
        if(!index.containsKey(term)) {
           Integer freq = 1;
           HashMap<Integer, Integer> pair = new HashMap<>();
           pair.put(docId, freq);
           index.put(term, pair);
        } else if (index.containsKey(term) && index.get(term).containsKey(docId)) {
            int counter =  index.get(term).get(docId);
            index.get(term).put(docId,++counter);
        } else {
            index.get(term).put(docId, 1);
        }
        
    }
    
    /**
     * Calculating term frequency, inverse document frequency
     */
    private void calculateTfIdf() {
        for(Entry elem: index.entrySet()) {
            HashMap<Integer, Integer> map = (HashMap<Integer, Integer>)elem.getValue();
            HashMap<Integer, Double> id_tfIdf = new HashMap<>();
            for(Entry pair: map.entrySet()) {   
             double tfIdf = (double)((Integer)pair.getValue() * 
                     Math.log(((double)FREE_DOC_ID/(double)map.size())));
             id_tfIdf.put((Integer)pair.getKey(), tfIdf); 
            }
        tf_Idf.put((String)elem.getKey(), id_tfIdf);
       }
    }
    
    /**
     * Creating vectors for documents
     */
    public void createVectors() {
        for(int i = 0; i < FREE_DOC_ID; i++) {
             Vector<Double> vec = new Vector<>();
            for(Entry elem: tf_Idf.entrySet()){
                HashMap<Integer, Double> map = (HashMap<Integer, Double>)elem.getValue();
                if(map.containsKey(i)) {
                    vec.add(map.get(i));
                }
            }
            Double vecLntg = 0d;
            for(Double elem: vec) {
                vecLntg += Math.pow(elem, 2);
            }
            vecLenght.put(i, Math.sqrt(vecLntg));
            docVec.put(i, vec);
        }
    }
    
    /**
     * Search based on cosine similarity
     * @param query Query
     */
    public void search(String query) {
        queryWords = query.toLowerCase().split(" ");
        if(queryWords.length > 1) {
            queryVector = queryToVector(queryWords);
            findCosineSimilarity(queryWords, queryVector);
        }
    }
    
    /**
     * Finding cosine similarity bettween query and documents
     * @param words Words of a query
     * @param queryVector Vector of a query
     */
    private void findCosineSimilarity(String [] words, Vector<Double> queryVector) {
        Double queryVectorLength = 0d;
            for(Double elem: queryVector) {
                queryVectorLength += Math.pow(elem, 2);
            }
            queryVectorLength = Math.sqrt(queryVectorLength);
        //dot products
        for (Integer docId: leaders) {
            double dotProduct = 0;
            for(String word: words) {
                if(tf_Idf.containsKey(word)) {
                    HashMap<Integer, Double> map = tf_Idf.get(word);
                    if(map.containsKey(docId)) {
                        double doc_word_tfIdf = (double)map.get(docId);
                        double query_word_idf = Math.log((double)FREE_DOC_ID/
                                (double)index.get(word).size());
                        dotProduct += doc_word_tfIdf * query_word_idf;
                    }
                }              
            }
            double cosineSimilarity = dotProduct/
                     (queryVectorLength*vecLenght.get(docId));
            cosineSimilarities.put(docId, cosineSimilarity);
        }
        cosineSimilarities = (HashMap<Integer, Double>)sortByComparator(cosineSimilarities);
    }
    
    /**
     * Finding folowers of a leading documents
     */
    private void findFollowers(){
        for(int docId = 0; docId<FREE_DOC_ID; docId++) {
            if(!leaders.contains(docId)) {
                TreeMap<Integer, Double> listOfCosSimilarities = new TreeMap<>();
                for(int leader:leaders ) {
                    listOfCosSimilarities.put(leader, findCosSimBetween2Docs(docId, leader));
                }
                LinkedHashMap<Integer, Double> newlistOfCosSimilarities = 
                        (LinkedHashMap)sortByComparator(listOfCosSimilarities);
                Set<Integer> set = newlistOfCosSimilarities.keySet();
                    Object [] keys  = set.toArray();
                    ArrayList<Integer> lst = new ArrayList<>();
                for(int i = 0; i< B1; i++) {
                    lst.add((Integer)keys[i]);
                } 
                followers.put(docId, lst);
            }
        }
    }
    
    /**
     * Finding cosine similarity between two documents
     * @param doc1 First document
     * @param doc2 Second document 
     * @return 
     */
    private double findCosSimBetween2Docs(int doc1, int doc2) {
        double dotProduct = 0;
        for(Entry elem: tf_Idf.entrySet()) {
            HashMap<Integer, Double> map = (HashMap<Integer, Double>)elem.getValue();
            if(map.containsKey(doc1) && map.containsKey(doc2)) {
                dotProduct += (double)map.get(doc1) * (double)map.get(doc2);
            }
        }
        return dotProduct/(vecLenght.get(doc1)*vecLenght.get(doc2));
    }
    
    /**
     * Transformation query to a vector
     * @param q Query
     * @return 
     */
    private Vector<Double> queryToVector(String [] q) {
        Vector<Double> vec = new Vector<>();
        for(String word: q) {
            if(index.containsKey(word)) {
                double idf = Math.log((double)FREE_DOC_ID/(double)index.get(word).size());
                vec.add(idf);
            }
        }
    return vec;
    }
    
    /**
     * Clustering
     */
    private void clustering() {
        Random rand = new Random(77);
        int sqrt_N = (int)Math.round(Math.sqrt(FREE_DOC_ID));
        for(int i = 0; i< sqrt_N; i++) {
            int k = rand.nextInt(FREE_DOC_ID);
            if(!leaders.contains(k))
                leaders.add(k);
            else sqrt_N++;
        }
        System.out.println("SQRT_N: " + leaders.size());
        findFollowers();
        
    }
   
    /**
     * Show a list of folowers and similarities
     */
    public void showSimilarity() {
        int i = 0;
        ArrayList<Integer> finalList = new ArrayList<>();
        for(Entry elem : cosineSimilarities.entrySet()) {
            finalList.add((Integer)elem.getKey());
            System.out.println("leader: ");
            System.out.print(getDocName((Integer)elem.getKey()) + " : { ");
            System.out.print(elem.getValue().toString());
            System.out.println(""); 
            // show followers
            System.out.println("followers:");
            for(Entry elem1 : followers.entrySet()) {
                if(((ArrayList<Integer>)elem1.getValue()).contains(elem.getKey())) {
                    System.out.println(" - " + getDocName((Integer)elem1.getKey()));
                      finalList.add((Integer)elem1.getKey());
                }
            }
            if(++i == B2) break;
        }
        finalCosSim(finalList);
        System.out.println("#####################\nWhole sorted list" );
        for(Entry elem : cosineSimilarities.entrySet()) {
            System.out.print(getDocName((Integer)elem.getKey()) + " : ");
            System.out.println(elem.getValue().toString());
        }
        System.out.println("#####################");
    }
    
    /**
     * Creating a sorted map with cosine similarities
     * @param lst 
     */
    private void finalCosSim(ArrayList<Integer> lst) {
         Double queryVectorLength = 0d;
            for(Double elem: queryVector) {
                queryVectorLength += Math.pow(elem, 2);
            }
            queryVectorLength = Math.sqrt(queryVectorLength);
            cosineSimilarities = new HashMap<>();
        //dot products
        for (Integer docId: lst) {
            double dotProduct = 0;
            for(String word: queryWords) {
                if(tf_Idf.containsKey(word)) {
                    HashMap<Integer, Double> map = tf_Idf.get(word);
                    if(map.containsKey(docId)) {
                        double doc_word_tfIdf = (double)map.get(docId);
                        double query_word_idf = Math.log((double)FREE_DOC_ID/
                                (double)index.get(word).size());
                        dotProduct += doc_word_tfIdf * query_word_idf;
                    }
                }              
            }
             double cosineSimilarity = dotProduct/(queryVectorLength*vecLenght.get(docId));
             cosineSimilarities.put(docId, cosineSimilarity);
        }
        cosineSimilarities =  (HashMap<Integer, Double>) sortByComparator(cosineSimilarities);
    }
    
    /**
     * Return document name by id
     * @param i
     * @return 
     */
    private String getDocName(int i) {
        return docIdList.get(i);
    }
    
    /**
     * Show leading document
     */
    public void showLeaders(){
        System.out.println("LEADERS: ");
        for(Integer elem : leaders) {
            System.out.print(getDocName(elem));
            System.out.println("");
        }
    }
    
    /**
     * Show followers
     */
     public void showFollowers(){
        System.out.println("FOLLOWERS: ");
        for(Entry elem : followers.entrySet()) {
            System.out.print(getDocName((Integer)elem.getKey()) + " : { ");
            System.out.print(elem.getValue().toString());
            System.out.println("");
        }
    }
     
/**
 * Comparator for sorting list with cosine similarities
 * @param unsortMap Map
 * @return 
 */
private static Map sortByComparator(Map unsortMap) { 
        List list = new LinkedList(unsortMap.entrySet());
        //sort list based on comparator
        Collections.sort(list, new Comparator() {
             public int compare(Object o1, Object o2) {
	           return ((Comparable) ((Map.Entry) (o2)).getValue())
	           .compareTo(((Map.Entry) (o1)).getValue());
             }
	});
        //put sorted list into map again
	Map sortedMap = new LinkedHashMap();
	for (Iterator it = list.iterator(); it.hasNext();) {
	     Map.Entry entry = (Map.Entry)it.next();
	     sortedMap.put(entry.getKey(), entry.getValue());
	}
	return sortedMap;
   }
}