package boolsearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author Walkman
 */
public class BoolSearch {
    public static void main(String[] args) throws UnsupportedEncodingException, 
    FileNotFoundException, Exception {
        Holder holder = new Holder();
        final File folder = new File("D:\\_docs2");
      
        holder.fillMatrix(folder);
        
        holder.boolSearch("Thinking in Java");
        holder.boolSearchByFraze("Java AND Bruce AND Eckel");
        holder.coordIndexSearch("Thinking in Java");
       
        holder.doubleWordSearch("Thinking in Java");
        holder.coordIndexSearch("Bruce Eckel");
    }
}
