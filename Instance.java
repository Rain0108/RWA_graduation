import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Instance {
    public int num_of_nodes;
    public int num_of_wavelength;
    public int num_of_requests;
    public int hop_limit;
    public int bandwidth_limit;
    public int[][] adj;
    public int[] wavelength;
    public ArrayList<Integer> nodes;
    public ArrayList<Request> requests;
    public Map<ArrayList<Integer>, Integer> res_bandwidth;
    public ArrayList<OMS_WL> OMS_Wavelength;
    public ArrayList<Integer> st;
    public ArrayList<Integer> en;
    public Instance(String filename){
        File file=new File(filename);
        BufferedReader br=null;
        try {
            FileReader fr = new FileReader(file);
            br = new BufferedReader(fr);
            ArrayList<String> edges = new ArrayList<>();
            ArrayList<String> wl = new ArrayList<>();
            ArrayList<String> request = new ArrayList<>();
            String str = br.readLine();
            String w0 = str.split(":")[1];
            w0 = w0.substring(1, w0.length()-1);
            String[] wls = w0.split(",");
            wavelength = new int[wls.length];
            for(int i=0;i<wls.length;i++){
                wavelength[i] = Integer.parseInt(wls[i]);
            }
            num_of_wavelength = wavelength.length;
            ArrayList<ArrayList<Integer>> arr = new ArrayList<>();
            str = br.readLine();
            while (str.length() != 0){
                String str1 = str.split(" ")[0];
                String str2 = str.split(" ")[1];
                edges.add(str1);
                str = br.readLine();
                str2 = str2.substring(1, str2.length()-1);
                String[] temp = str2.split(",");
                ArrayList<Integer> w = new ArrayList<>();
                for (String s : temp) {
                    w.add(Integer.parseInt(s));
                }
                arr.add(w);
            }
            Map<Integer, Integer> map_of_nodes = new HashMap<>();
            int flag = 0;
            st = new ArrayList<>();
            en = new ArrayList<>();
            for (String edge : edges) {
                String temp = edge.substring(1, edge.length() - 1);
                int temp_s = Integer.parseInt(temp.split(",")[0]);
                int temp_e = Integer.parseInt(temp.split(",")[1]);
                st.add(temp_s);
                en.add(temp_e);
                if (!map_of_nodes.containsValue(temp_s)) {
                    map_of_nodes.put(flag++, temp_s);
                }
                if (!map_of_nodes.containsValue(temp_e)) {
                    map_of_nodes.put(flag++, temp_e);
                }
            }
            nodes = new ArrayList<>(map_of_nodes.size());
            for(int i=0;i<map_of_nodes.size();i++){
                nodes.add(map_of_nodes.get(i));
            }
            Collections.sort(nodes);
            num_of_nodes = nodes.size();
            str = br.readLine();
            String hop = str.split(":")[1];
            hop_limit = Integer.parseInt(hop);
            str = br.readLine();
            String band = str.split(":")[1];
            bandwidth_limit = Integer.parseInt(band);
            str = br.readLine();
            adj = new int[nodes.size()][nodes.size()];
            res_bandwidth = new HashMap<>();
            OMS_Wavelength = new ArrayList<>();
            for(int i=0;i<st.size();i++){
                try {
                    adj[st.get(i)][en.get(i)] = 1;
                }catch (ArrayIndexOutOfBoundsException e){
                    System.out.println(1);
                }
                adj[en.get(i)][st.get(i)] = 1;
                ArrayList<Integer> node_pair = new ArrayList<>(2);
                node_pair.add(st.get(i));
                node_pair.add(en.get(i));
                Collections.sort(node_pair);
                res_bandwidth.put(node_pair, bandwidth_limit);
            }
            for(int i=0;i<st.size();i++){
                OMS_Wavelength.add(new OMS_WL(st.get(i), en.get(i), arr.get(i)));
                OMS_Wavelength.add(new OMS_WL(en.get(i), st.get(i), arr.get(i)));
            }
            str = br.readLine();
            while (str != null){
                request.add(str);
                str = br.readLine();
            }
            requests = new ArrayList<>(request.size());
            for (String req : request) {
                String pair = req.split(" ")[0];
                try {

                    pair = pair.substring(1, pair.length() - 1);
                }catch (StringIndexOutOfBoundsException s){
                    System.out.println();
                }
                int s = Integer.parseInt(pair.split(",")[0]);
                int e = Integer.parseInt(pair.split(",")[1]);
                int r = Integer.parseInt(req.split(" ")[1]);
                requests.add(new Request(s, e, r));
            }
            num_of_requests = requests.size();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                if(br != null){
                    br.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

}
class OMS_WL{
    //每条OMS链路的波长集合
    int start;
    int end;
    ArrayList<Integer> wavelength;
    public OMS_WL(int start, int end, ArrayList<Integer> wavelength){
        this.start = start;
        this.end = end;
        this.wavelength = wavelength;
    }
}
