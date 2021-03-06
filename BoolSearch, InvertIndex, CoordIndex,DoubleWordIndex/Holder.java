package boolsearch;

import java.io.*;
import java.util.Map.Entry;
import java.util.*;

/**
 *
 * @author Walkman
 */
public class Holder {
    private static int FREE_DOC_ID;
    private int docNumber;
    private HashMap<String, BitSet> matrix;
    private ArrayList<String> docIdList;
    private HashMap<String, ArrayList<Integer>> invertIndex;
    private HashMap<String, Set<Integer>> doubleWordIndex;
    private HashMap<String, HashMap<Integer,ArrayList<Integer>>> coordIndex;

    public Holder () { 
        matrix = new HashMap<>();
        docIdList = new ArrayList();
        invertIndex = new HashMap<>();
        doubleWordIndex = new HashMap<>();
        coordIndex = new HashMap<>();
        FREE_DOC_ID = 0;
        docNumber = 0;
    }
    
   /**
    * Filling the incidence matrix
    * @param folder - Folder with documents 
    * @throws FileNotFoundException
    * @throws UnsupportedEncodingException 
    */
   public void fillMatrix(final File folder) throws FileNotFoundException,
   UnsupportedEncodingException {
        docNumber = folder.list().length;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                fillMatrix(fileEntry);
                docNumber += fileEntry.list().length;
            } else {
                docIdList.add(fileEntry.getName());
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(fileEntry.getAbsoluteFile()), "UTF-8"));
                String content = new Scanner(in).useDelimiter("\\Z").next();

                String firstName = content.substring(
                        content.indexOf("<first-name>")+12,
                        content.indexOf("</first-name>"));
                String lastName = content.substring(
                        content.indexOf("<last-name>")+11,
                        content.indexOf("</last-name>"));
                this.addToMatrix(firstName.toLowerCase(), FREE_DOC_ID);
                this.addToMatrix(lastName.toLowerCase(), FREE_DOC_ID);
                this.addToDoubleWordIndex(firstName.toLowerCase(), FREE_DOC_ID);
                this.addToDoubleWordIndex(lastName.toLowerCase(), FREE_DOC_ID);

                String [] bookTitle = content.substring(
                        content.indexOf("<book-title>")+12,
                        content.indexOf("</book-title>")).split(" ");
                for (int i = 0; i < bookTitle.length; i++) {
                    this.addToMatrix(bookTitle[i].toLowerCase(), FREE_DOC_ID);
                    this.addToDoubleWordIndex(bookTitle[i].toLowerCase(), FREE_DOC_ID);
                    this.addToDoubleWordIndex(bookTitle[i].toLowerCase(), FREE_DOC_ID);
                }

                String body = content.substring(content.indexOf("<body>")+6,
                        content.indexOf("</body>"));
                body = body.replaceAll("</v><v>", " ");
                body = body.replaceAll("\\<.*?\\>", "");
                body = body.replaceAll("\n", " ");
                body = body.replaceAll("[\\W]|_", " ");

                String[] line = body.split(" ");
                    for (int i = 0; i < line.length-1; i++) {
                        this.addToMatrix(line[i], FREE_DOC_ID);
                        this.addToCoordIndex(line[i], FREE_DOC_ID, i);
                        this.addToDoubleWordIndex(line[i]+" "+line[i+1], FREE_DOC_ID);
                    }
                 this.addToMatrix(line[line.length-1], FREE_DOC_ID);
                 this.addToCoordIndex(line[line.length-1], FREE_DOC_ID, line.length-1);
                    FREE_DOC_ID++;
                }
            }
        createInvertIndex();
    }

   /**
    * Add one word to incidence matrix
    * @param elem Word
    * @param docId Number of document
    */
    public void addToMatrix(String elem, int docId) {
        if(!matrix.containsKey(elem)) {
            BitSet docs = new BitSet(docNumber);
            for(int i = 0; i<docNumber; i++) {
                docs.set(i, false);
            }
            docs.set(docId, true);
            matrix.put(elem, docs);
        } else {
           matrix.get(elem).set(docId, true);
        }
    }
    
    /**
     * Add word to double-word index
     * @param elem Word
     * @param docId Number of document
     */
    public void addToDoubleWordIndex(String elem, int docId) {
        if(!doubleWordIndex.containsKey(elem)) {
            Set docs = new HashSet();
            docs.add(docId);
            doubleWordIndex.put(elem, docs);
        } else {
           doubleWordIndex.get(elem).add(docId);
        }
    }
    
    /**
     * Add word to coordinate index
     * @param elem Word
     * @param docId Number of document
     * @param position Position of a word in document
     */
    public void addToCoordIndex(String elem, int docId, int position) {
        if(!coordIndex.containsKey(elem)) {
            HashMap<Integer, ArrayList<Integer>> docs = new HashMap();
            ArrayList<Integer> coords = new ArrayList();
            coords.add(position);
            docs.put(docId, coords);
            coordIndex.put(elem, docs);
        } else if (!coordIndex.get(elem).containsKey(docId)) {
            ArrayList<Integer> coords = new ArrayList();
            coords.add(position);
            coordIndex.get(elem).put(docId, coords);
        }else{ 
           coordIndex.get(elem).get(docId).add(position);
        }
    }
    
    /**
     * Bool searh on a native language
     * @param query Query
     * @throws Exception 
     */
    public void boolSearch(String query) throws Exception {
        String text = query.replaceAll(" +", " ").replaceAll(" ", " AND ");
           boolSearchByFraze(text);
    }
    
    /**
     * Bool searh with boolean operators AND, OR, NOT
     * @param query Query
     * @throws Exception 
     */
    public void boolSearchByFraze(String query) throws Exception {
        boolean skip = false;
        ArrayList<BitSet> terms = new ArrayList<>();
        LinkedList<String> operators = new LinkedList<>();
        String[] line = query.split(" ");
        for(int i = 0; i<line.length; i++) {
            if(line[i].matches("NOT")) {
                if(matrix.containsKey(line[i+1])) {
                    terms.add(invert(matrix.get(line[i+1])));
                    i++;
                } else {
                    skip = true;
                }
            }
            else if(line[i].matches("AND") || line[i].matches("OR")) {
                if(!skip) {
                    operators.add(line[i]);
                    skip = false;
                }
            } else {
                if(matrix.containsKey(line[i]))
                    terms.add(matrix.get(line[i]));
                else {
                    skip = true;
                }
            }
        }
        
        if (terms.isEmpty()) {
            System.out.println("Try another query");
            System.exit(0);
        }
        
        if(terms.size() == 1) {
             BitSet result = terms.get(0);
             System.out.println("Query: " + query);
             System.out.println("Result: ");
             for(int i = 0; i<docNumber; i++) {
                if (result.get(i) == true)
                    System.out.println(getDocName(i));
            }
        } else {
            BitSet result = terms.get(0);
            int j = 1;
            for (String elem : operators) {
                if (elem.matches("AND")) {
                    result.and(terms.get(j));
                    j++;
                }
                else if(elem.matches("OR")) {
                    result.or(terms.get(j));
                    j++;
                }
            }
            System.out.println("Query: " + query);
            System.out.println("Result: ");
            for(int i = 0; i<docNumber; i++) {
                if (result.get(i) == true)
                    System.out.println(getDocName(i));
            }
        }
    }
    
    /**
     * Search by double-word index
     * @param query Query
     * @throws Exception 
     */
    public void doubleWordSearch(String query) throws Exception {
        String[] line = query.split(" ");
        Set<Integer> result = new HashSet();
        
        if (line.length == 0) {
            System.out.println("Try another query");
            System.exit(0);
        } else if(line.length == 1) {
             boolSearch(query);
        } else {
            if (doubleWordIndex.containsKey(line[0]+" "+line[1]))
                result = doubleWordIndex.get(line[0]+" "+line[1]);
            for (int i = 1; i<line.length; i++) {
                if (i+1 < line.length) {
                    if (doubleWordIndex.containsKey(line[i]+" "+line[i+1]))
                        result.retainAll(doubleWordIndex.get(line[i]+" "+line[i+1]));
                }
            }
        
            System.out.println("Query: " + query);
            System.out.println("Result: ");
            for (Integer docId: result) {
                System.out.println(getDocName(docId));
            }  
        }   
    }
    
    /**
     * Search by coordinate index
     * @param query Query
     * @throws Exception 
     */
    public void coordIndexSearch(String query) throws Exception {
        String[] line = query.split(" ");
        HashMap<Integer, ArrayList<Integer>> result = new HashMap();
         
        if (line.length == 0) {
            System.out.println("Try another query");
            System.exit(0);
        } else if(line.length == 1) {
             boolSearch(query);
        } else {
            if (coordIndex.containsKey(line[0]))
                result = coordIndex.get(line[0]);
            for (int i = 1; i<line.length; i++) {
                if (coordIndex.containsKey(line[i]))
                    result.keySet().retainAll(coordIndex.get(line[i]).keySet()); 
            }
        
            System.out.println("Query: " + query);
            System.out.println("Result: ");
            Boolean res = false;
            for (Integer docId: result.keySet()) {
                ArrayList<Integer> coordList1 = coordIndex.get(line[0]).get(docId);
                for(int i = 1; i<line.length-1; i++) {
                    ArrayList<Integer> coordList2 = coordIndex.get(line[i]).get(docId);
                    res = (intersect(coordList1, coordList2, i) ? true : false);
                }
                if(res) {
                    System.out.println(getDocName(docId));
                }   
            }  
        }   
    }
    
    /**
     * Utility method to intersect two coordinate lists
     * @param list1 First list
     * @param list2 Second list
     * @param k length
     * @return 
     */
    private boolean intersect(ArrayList<Integer> list1, ArrayList<Integer> list2, int k) {
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()) {
           if(list1.get(i) == list2.get(j)-k)
               return true;
           else if(list1.get(i) < list2.get(j)) 
               i++;
           else
               j++;
       }
    return false;
    }
    
    /**
     * Utility method to invert BitSet
     * @param in Input BitSet
     * @return 
     */
    private BitSet invert(BitSet in) {
        BitSet docs = new BitSet(docNumber);
            for(int i = 0; i < docNumber; i++) {
                docs.set(i, !in.get(i));
            }
    return docs;
    }
    
    /**
     * Building invert index form incidence matrix
     */
    private void createInvertIndex() {
        for(Entry elem : matrix.entrySet()) {
            ArrayList<Integer> docIds= new ArrayList<>();
            BitSet docs = (BitSet) elem.getValue();
            for(int i = 0; i < docNumber; i++) {
                if (docs.get(i) == true)
                    docIds.add(i);
            }
            invertIndex.put(elem.getKey().toString(), docIds);
        }
    }
    
    /**
     * Return document name by id
     * @param id Document id
     * @return 
     */
    private String getDocName(int id) {
        return docIdList.get(id);
    }
    
    private Exception NoSuchElementException() {
        throw new UnsupportedOperationException("No such element");
    }
}
