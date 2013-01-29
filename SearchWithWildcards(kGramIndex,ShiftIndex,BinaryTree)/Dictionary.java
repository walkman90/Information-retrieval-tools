package wildcards;

import java.io.*;
import java.util.*;

/**
 *
 * @author Walkman
 */
public class Dictionary {
    private static int FREE_DOC_ID;
    private int docNumber;
    private ArrayList<String> docIdList;
    private HashMap<String, Set<Integer>> invertIndex;
    private ArrayList<String> shiftIndex;
    private HashMap<String, ArrayList<String>> kGramIndex;
    private BinaryTree binTree;
    
    public Dictionary () { 
        docIdList = new ArrayList();
        invertIndex = new HashMap<>();
        shiftIndex = new ArrayList<>();
        kGramIndex = new HashMap<>();
        binTree = new BinaryTree();
        FREE_DOC_ID = 0;
        docNumber = 0;
    }
    
    /**
     * Building all indexes and binary tree
     * @param folder Folder with documents
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    public void buildIndexes(final File folder) throws FileNotFoundException,
    UnsupportedEncodingException, IOException {
        docNumber = folder.list().length;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                buildIndexes(fileEntry);
                docNumber += fileEntry.list().length;
            } else {
                docIdList.add(fileEntry.getName());
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(fileEntry.getAbsoluteFile()), "UTF-8"));
                String content = new Scanner(in).useDelimiter("\\Z").next();

                String body = content.substring(content.indexOf("<body>")+6,
                        content.indexOf("</body>"));
                body = body.replaceAll("</v><v>", " ");
                body = body.replaceAll("\\<.*?\\>", "");
                body = body.replaceAll("\n", " ");
                body = body.replaceAll("[\\W]|_", " ");

                String[] line = body.split(" ");
                for (int j = 0; j < line.length; j++) {
                    this.addToInvertIndex(line[j], FREE_DOC_ID);
                    this.addToShiftIndex(line[j]);
                    this.addTokGramIndex(line[j]);
                    this.binTree.insert(binTree.getRootNode(), line[j]);
                }
                FREE_DOC_ID++;   
            }
        }
    }
    
    /**
     * Adding word to invert index
     * @param term Word
     * @param docId Document id
     */
    private void addToInvertIndex (String term, int docId) {
        if(!invertIndex.containsKey(term)) {
            Set<Integer> docs = new HashSet<>(docNumber);
            docs.add(docId);
            invertIndex.put(term, docs);
        } else {
           invertIndex.get(term).add(docId);
        }
    }
    
    /**
     * Adding word to shifted index
     * @param term Word
     */
    private void addToShiftIndex(String term) {
        String str = term + '$';
        if (!shiftIndex.contains(str)) {
            shiftIndex.add(str);
            for (int i = 1; i < str.length(); i++) {
                shiftIndex.add(str.substring(i)+str.substring(0, i));
            }
        }
    }
    
    /**
     * Adding word to k-gram index
     * @param str Word
     */
    private void addTokGramIndex(String str) {
        String term = '$' + str + '$';
        String kgram = "";
        for (int i = 0; i < str.length(); i++) {
            kgram = term.substring(i, i+3);
            if (!kGramIndex.containsKey(kgram)) {
                ArrayList<String> terms = new ArrayList<>();
                terms.add(str);
                kGramIndex.put(kgram, terms);
            } else if (!kGramIndex.get(kgram).contains(str)) {
                kGramIndex.get(kgram).add(str);
            }
        }     
     }
    
     /**
      * Utility method to get orogonal word from shifted word
      * @param shiftedWord Shifted word
      * @return 
      */
    private String getWordFromShifted(String shiftedWord) {
        String result = shiftedWord;
        if (result.indexOf('$') == 0 ) {
            return result.substring(1);
        } else if (result.indexOf('$') == result.length()-1 ) {
            return result.substring(0, result.length()-1);
        } else {
            int i = 0;
            while (result.indexOf('$') != 0) {
                result = result.substring(i)+result.substring(0, i);
                i++;
            }
        }
        return result.substring(1, result.length());
    }
    
     /**
      * Searh with wildcards
      * @param query Query
      */
    public void searchWithWildcards(String query){
        System.out.println("Query: " + query);
        query += '$';
        if (query.indexOf('*') == query.lastIndexOf('*')) {
            int i = 1;
            while (query.indexOf('*') != query.length()-1) {
                query = query.substring(1)+query.substring(0, 1);
                i++;
            }
            query = query.substring(0, query.length()-1);
            printResult(searchWithOneWildcards(query));
        } else {
            printResult(searchWithTwoWildcards(query));
        }
    }
    
    /**
     * Utility method for searching with one wildcard
     * @param query Query
     * @return 
     */
    private ArrayList<String> searchWithOneWildcards(String query) {
        ArrayList<String> res = new ArrayList<>();
            query = query.replace("$", "\\$");
            String regexp = query+"[0-9a-zA-Z]+";
            for(String elem: shiftIndex) {
                if (elem.matches(query+"\\w+")) {
                    res.add(elem);
                }
            }
        return res;
    }
    
    /**
     * Utility method for searching with two wildcards
     * @param query Query
     * @return 
     */
    private ArrayList<String> searchWithTwoWildcards(String query) {
        ArrayList<String> res = new ArrayList<>();
        String middlePart = query.substring(
                query.indexOf('*')+1, query.lastIndexOf('*'));
        int i = 0;
        while (query.lastIndexOf('*') != query.length()-1) {
            query = query.substring(1)+query.substring(0, 1);
            i++;
        }
        query = query.substring(query.indexOf('*')+1, query.lastIndexOf('*'));
        for (String elem: searchWithOneWildcards(query)) {
            if(elem.contains(middlePart)) {
                res.add(elem);
            }
        }
        return res;
    }
    
    /**
     * Utility methof for printing result
     * @param list Result list
     */
    private void printResult (ArrayList<String> list) {
        System.out.println("Result: ");
        for(String elem: list) {
            String answer = getWordFromShifted(elem);
            System.out.print("# " + answer);
            System.out.println(" : " + invertIndex.get(answer).toString());
        }
    }
    
    /**
     * Utility method that return the root of a binary tree
     * @return 
     */
    public BinaryTree.Node getRoot() {
        return binTree.getRootNode();
    }
    
    /**
     * Utility method that prints binary tree in right order
     * @param node 
     */
    public void printInOrder(BinaryTree.Node node) {
        if (node != null) {
        printInOrder(node.left);
        System.out.println(node.value);
        printInOrder(node.right);
        }
    }
}


