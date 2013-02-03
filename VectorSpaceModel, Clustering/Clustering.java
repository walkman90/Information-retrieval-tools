package clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author Walkman
 */
public class Clustering {
    public static void main(String[] args) throws FileNotFoundException, 
    UnsupportedEncodingException, IOException, ClassNotFoundException {
        final File folder = new File("D:\\_docs2");
         
        Holder h = new Holder();
        h.buildIndexes(folder);
        
        h.search("Thinking in Java");
        h.showSimilarity();
        h.showLeaders();
        h.showFollowers();
    }
}
