import java.util.ArrayList;

public class OCH_route {
    public ArrayList<Integer> och;
    public int head = -1;
    public int tail = -1;
    public OCH_route(int[] nodes, int head, int tail){
        och = new ArrayList<>(nodes.length);
        for(int i : nodes) och.add(i);
        this.head = head;
        this.tail = tail;
    }
    public OCH_route(ArrayList<Integer> nodes, int head, int tail){
        och = new ArrayList<>(nodes.size());
        och.addAll(nodes);
        this.head = head;
        this.tail = tail;
    }
}
