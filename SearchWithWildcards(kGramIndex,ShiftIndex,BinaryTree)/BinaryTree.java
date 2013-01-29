package wildcards;

/**
 *
 * @author Walkman
 */
 public class BinaryTree {
    private Node rootNode;
    
    public BinaryTree() {
        rootNode = new Node("");
    }
    
    static class Node {
        Node left;
        Node right;
        String value;

        public Node(String value) {
            this.value = value;
        }
    }
    
    /**
     * Getting root node of a tree
     * @return 
     */
    public Node getRootNode() {
        return rootNode;
    }
    
    /**
     * INderting node into binary tree
     * @param node Node
     * @param value Value
     */
    public void insert(Node node, String value) {
        if (value.compareToIgnoreCase(node.value) < 0) {
            if (node.left != null) {
                insert(node.left, value);
            } else {
                node.left = new Node(value);
            }
        } else if (value.compareToIgnoreCase(node.value) > 0 ) {
            if (node.right != null) {
                insert(node.right, value);
            } else {
                node.right = new Node(value);
            }
        }
    }  
}
