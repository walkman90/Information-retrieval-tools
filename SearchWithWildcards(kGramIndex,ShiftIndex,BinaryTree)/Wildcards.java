package wildcards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author Walkman
 */
public class Wildcards {
    public static void main(String[] args) throws FileNotFoundException, 
    UnsupportedEncodingException, IOException {
        Dictionary dict = new Dictionary();
        final File folder = new File("D:\\_docs2");
        
        dict.buildIndexes(folder);
        dict.searchWithWildcards("Ja*a");
        dict.searchWithWildcards("exc*pti*n");
//      System.out.print("Print tree in order: ");
//      dict.printInOrder(dict.getRoot());
    }
}
