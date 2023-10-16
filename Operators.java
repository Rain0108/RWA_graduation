import java.util.*;

public class Operators {
    public Instance instance;
    public Solution solution;
    Random random = new Random();
    public Operators(Instance instance){
        this.instance=instance;
    }
    public boolean isFeasible(Solution solution, Instance instance){
    //可行条件：所有路径都连通，所有需求都被满足, 剩余带宽都不小于0，波长分配方案可行
        if(!solution.wl_assign_right) return false;
        for(int i=0;i<solution.service_links.size();i++){
            for(int j=0;j<solution.service_links.get(i).och.size()-1;j++){
                if(instance.adj[solution.service_links.get(i).och.get(j)][solution.service_links.get(i).och.get(j+1)]
                        == 0) return false;
            }
        }
        for(int i=0;i<instance.num_of_requests;i++){
            boolean flag = false;
            for(int j=0;j<solution.service_links.size();j++){
                if(instance.requests.get(i).start == solution.service_links.get(j).head &&
                        instance.requests.get(i).end == solution.service_links.get(j).tail){
                    flag = true;
                    break;
                }
            }
            if(!flag) return false;
        }
        for(ArrayList<Integer> arrayList : solution.res_bandwidth.keySet()){
            if(solution.res_bandwidth.get(arrayList) < 0) return false;
        }
        /*
        for(int i=0;i<solution.service_links.size();i++){
            if(!OCH_WL_feasible(instance, solution.service_links.get(i), solution.OMS_Wavelength)) return false;
        }
        */
        return isAssignTrue(instance, solution);
    }
    public boolean isAssignTrue(Instance instance, Solution solution){
        ArrayList<OMS_WL> OMS_Wavelength = new ArrayList<>();
        for(int i=0;i<instance.OMS_Wavelength.size();i++){
            OMS_Wavelength.add(new OMS_WL(instance.OMS_Wavelength.get(i).start, instance.OMS_Wavelength.get(i).end,
                    new ArrayList<>(instance.OMS_Wavelength.get(i).wavelength)));
        }
        for(int i=0;i<solution.service_links.size();i++){
            for(int j=0;j<solution.service_links.get(i).och.size()-1;j++){
                int st = solution.service_links.get(i).och.get(j);
                int en = solution.service_links.get(i).och.get(j+1);
                for(int k=0;k<OMS_Wavelength.size();k++){
                    if((OMS_Wavelength.get(k).start == st && OMS_Wavelength.get(k).end == en) ||
                            ((OMS_Wavelength.get(k).start == en && OMS_Wavelength.get(k).end == st))){
                        if(!OMS_Wavelength.get(k).wavelength.contains(solution.wl_assignment.get(i))){
                            return false;
                        }
                        OMS_Wavelength.get(k).wavelength.remove(solution.wl_assignment.get(i));
                    }
                }
            }
        }
        return true;
    }
    public boolean isBeyond(int node, OCH_route route){
        //判断加上route后的路径是否超过终点node（经过且终点不为node）
        if(route.och.size() == 2){
            if(route.head == node) return true;
        }
        for(int i=1;i<route.och.size()-1;i++){
            if(route.och.get(i) == node) return true;
        }
        return false;
    }

    public boolean myContainValue(ArrayList<Integer> arr, Map<Integer, ArrayList<Integer>> och_dict){
        //判断字典中是否存在目标值
        Collections.sort(arr);
        for(int i=0;i<och_dict.size();i++){
            if(arr.size() != och_dict.get(i).size()) continue;
            int con = 0;
            for(int j=0;j<arr.size();j++){
                if(arr.get(j).equals(och_dict.get(i).get(j))) con++;
            }
            if(con == arr.size()) return true;
        }
        return false;
    }

    public boolean isRepeat(ArrayList<OCH_route> cur_route, OCH_route route){
        Map<Integer, Integer> visited = new HashMap<>();
        int flag = 0;
        for(int i=0;i<cur_route.size();i++){
            for(int j=0;j<cur_route.get(i).och.size();j++){
                visited.put(flag++, cur_route.get(i).och.get(j));
            }
        }
        for(int i=1;i<route.och.size();i++){
            if(visited.containsValue(route.och.get(i))) return true;
        }
        return false;
    }

    public OCH_route OCH_sort(OCH_route route){
        int cur_node = route.head;
        ArrayList<Integer> nodes = new ArrayList<>(route.och.size());
        Map<Integer, Boolean> visited = new HashMap<>();
        for(Integer i : route.och) visited.put(i, false);
        visited.put(route.head, true);
        nodes.add(route.head);
        while (cur_node != route.tail){
            for(int i=0;i<route.och.size();i++){
                if(instance.adj[cur_node][route.och.get(i)] == 1 && !visited.get(route.och.get(i))){
                    cur_node = route.och.get(i);
                    visited.put(cur_node, true);
                    nodes.add(cur_node);
                    break;
                }
            }
        }
        return new OCH_route(nodes, route.head, route.tail);
    }

    public boolean OCH_compare(boolean isReverse, OCH_route route1, OCH_route route2){
        //isReverse为真时将0-1-2与2-1-0这类视为同一个OCH
        if(route1.och.size() != route2.och.size()) return false;
        if(!isReverse) {
            if (route1.head != route2.head || route1.tail != route2.tail) return false;
            for (int i = 1; i < route1.och.size(); i++) {
                if (!route1.och.get(i).equals(route2.och.get(i))) return false;
            }
        }
        else{
            if (route1.head == route2.tail && route1.tail == route2.head){
                for(int i=1;i<route1.och.size();i++){
                    if(!route1.och.get(i).equals(route2.och.get(route1.och.size()-1-i))) return false;
                }
            }
            else{
                for (int i = 0; i < route1.och.size(); i++) {
                    if (!route1.och.get(i).equals(route2.och.get(i))) return false;
                }
            }
        }
        return true;
    }

    public ArrayList<OCH_route> get_all_OCH(Operators operators, Instance instance){
        //分别从每个点出发，找到所有OCH
        ArrayList<ArrayList<Integer>> all_OCH = new ArrayList<>();
        Map<Integer, ArrayList<Integer>> och_dict = new HashMap<>();
        ArrayList<Special_node> special_nodes = new ArrayList<>(instance.nodes.size());
        for(int i=0;i<instance.nodes.size();i++){
            special_nodes.add(new Special_node(instance.nodes.get(i), 0, -1));
        }
        int flag = 0;
        for(int i=0;i<instance.nodes.size();i++){
            Queue<Special_node> node_queue = new LinkedList<>();
            ArrayList<Special_node> node_layer = new ArrayList<>();
            boolean[] isVisited = new boolean[instance.adj.length];
            node_queue.offer(special_nodes.get(i));
            int current_layer = 1;
            int current_previous = -1;
            while(true){
                //首位出队
                Special_node front = node_queue.poll();
                if(current_layer > instance.hop_limit) break;
                node_layer.add(front);
                assert front != null;
                isVisited[front.num] = true;
                //邻居入队
                for(int j=0;j<instance.adj[0].length;j++){
                    if(instance.adj[front.num][j] != 0 && !isVisited[j]){
                        node_queue.offer(new Special_node(j, current_layer + 1, front.num));
                        ArrayList<Integer> current_och = new ArrayList<>();
                        Special_node front_temp = front;
                        current_och.add(j);
                        while(front_temp.previous != -1){
                            current_och.add(front_temp.num);
                            for(Special_node s : node_layer){
                                if(s.num == front_temp.previous){
                                    front_temp = s;
                                    break;
                                }
                            }
                        }
                        current_och.add(i);
                        Collections.sort(current_och);
                        if(!operators.myContainValue(current_och, och_dict)){
                            och_dict.put(flag++, current_och);
                        }
                    }
                }
                if(node_queue.size() == 0) break;
                if(node_queue.peek().previous != current_previous){
                    current_layer ++;
                    current_previous = node_queue.peek().previous;
                }
            }

        }

        for(int i=0;i<och_dict.size();i++){

            all_OCH.add(och_dict.get(i));
        }
        return OCH_transform(all_OCH, instance.OMS_Wavelength);
    }

    public ArrayList<OCH_route> OCH_transform(ArrayList<ArrayList<Integer>> all_och, ArrayList<OMS_WL> OMS_Wavelength){
        //将och链路转化成标准形式并去除波长不可行的
        ArrayList<OCH_route> res = new ArrayList<>();
        for(int m=0;m<all_och.size();m++) {
            ArrayList<Integer> head_tail = new ArrayList<>(2);
            for (int i = 0; i < all_och.get(m).size(); i++) {
                int n_count = 0;
                for (int j = 0; j < all_och.get(m).size(); j++) {
                    if (instance.adj[all_och.get(m).get(i)][all_och.get(m).get(j)] == 1) n_count++;
                }
                if(n_count == 1) head_tail.add(all_och.get(m).get(i));
            }
            //排除och链路中的环
            if(head_tail.size() != 2) continue;
            OCH_route res1 = OCH_sort(new OCH_route(all_och.get(m), head_tail.get(0), head_tail.get(1)));
            if(OCH_WL_feasible(instance, res1, OMS_Wavelength)){
                res.add(res1);
                res.add(OCH_sort(new OCH_route(all_och.get(m), head_tail.get(1), head_tail.get(0))));
            }
        }
        return res;
    }

    public boolean OCH_WL_feasible(Instance instance, OCH_route OCH, ArrayList<OMS_WL> OMS_Wavelength){
        ArrayList<Integer> st = new ArrayList<>();
        ArrayList<Integer> en = new ArrayList<>();
        ArrayList<Integer> temp = new ArrayList<>(OCH.och);
        for(int i=0;i<temp.size()-1;i++){
            st.add(temp.get(i));
            en.add(temp.get(i+1));
        }
        int[][] wl = new int[st.size()][instance.wavelength.length];
        ArrayList<ArrayList<Integer>> wls = new ArrayList<>();
        for(int i=0;i<st.size();i++){
            for (OMS_WL oms_wl : OMS_Wavelength) {
                if (st.get(i) == oms_wl.start &&
                        en.get(i) == oms_wl.end) {
                    wls.add(new ArrayList<>(oms_wl.wavelength));
                    break;
                }
            }
        }
        for(int i=0;i<wls.size();i++){
            for(int j=0;j<wl[0].length;j++){
                if(wls.get(i).contains(instance.wavelength[j])) wl[i][j] = 1;
            }
        }
        for(int i=0;i<wl[0].length;i++){
            int f = 0;
            for (int[] ints : wl) {
                f += ints[i];
            }
            if(f == wl.length) return true;
        }
        return false;
    }

    public int getCost(Solution solution){

        ArrayList<OCH_route> arr = new ArrayList<>();
        arr.add(solution.OCH_links.get(0).get(0));
        for(int i=0;i<solution.OCH_links.size();i++){
            for(int j=0;j<solution.OCH_links.get(i).size();j++){
                boolean flag = false;
                for(int k=0;k<arr.size();k++){
                    if(OCH_compare(true, arr.get(k), solution.OCH_links.get(i).get(j))){
                        flag = true;
                        break;
                    }
                }
                if(!flag) arr.add(solution.OCH_links.get(i).get(j));
            }
        }
        boolean[] flag = new boolean[arr.size()];
        ArrayList<OCH_route> long_OCH = new ArrayList<>();
        for(int i=0;i<arr.size();i++){
            if(arr.get(i).och.size() > 2) long_OCH.add(arr.get(i));
        }
        for(int i=0;i<arr.size();i++){
            if(arr.get(i).och.size() > 2) continue;
            for(int j=0;j< long_OCH.size();j++){
                if(OCH_pair(arr.get(i), long_OCH.get(j))) flag[i] = true;
            }
        }
        /*
        for(int i=1;i<arr.size();i++){
            for(int j=0;j<i;j++){
                if(arr.get(i).head == arr.get(j).head){
                    ArrayList<Integer> temp = new ArrayList<>(arr.get(i).och);
                    temp.remove(0);
                    Collections.reverse(temp);
                    temp.addAll(arr.get(j).och);
                    OCH_route newOCH = new OCH_route(temp, temp.get(0), temp.get(temp.size()-1));
                    for(int k=0;k<arr.size();k++){
                        if(OCH_compare(true, newOCH, arr.get(k))){
                            flag[i] = true;
                            flag[j] = true;
                        }
                    }
                }
                if(arr.get(i).head == arr.get(j).tail){
                    //021 60
                    ArrayList<Integer> temp = new ArrayList<>(arr.get(i).och);
                    temp.remove(0);
                    Collections.reverse(temp);
                    ArrayList<Integer> temp1 = new ArrayList<>(arr.get(j).och);
                    Collections.reverse(temp1);
                    temp.addAll(temp1);
                    OCH_route newOCH = new OCH_route(temp, temp.get(0), temp.get(temp.size()-1));
                    for(int k=0;k<arr.size();k++){
                        if(OCH_compare(true, newOCH, arr.get(k))){
                            flag[i] = true;
                            flag[j] = true;
                        }
                    }
                }
                if(arr.get(i).tail == arr.get(j).tail){
                    //021 61
                    ArrayList<Integer> temp = new ArrayList<>(arr.get(i).och);
                    temp.remove(temp.size()-1);
                    ArrayList<Integer> temp1 = new ArrayList<>(arr.get(j).och);
                    Collections.reverse(temp1);
                    temp.addAll(temp1);
                    OCH_route newOCH = new OCH_route(temp, temp.get(0), temp.get(temp.size()-1));
                    for(int k=0;k<arr.size();k++){
                        if(OCH_compare(true, newOCH, arr.get(k))){
                            flag[i] = true;
                            flag[j] = true;
                        }
                    }
                }
                if(arr.get(i).tail == arr.get(j).head){
                    //01 16
                    ArrayList<Integer> temp = new ArrayList<>(arr.get(i).och);
                    temp.remove(temp.size()-1);
                    temp.addAll(arr.get(j).och);
                    OCH_route newOCH = new OCH_route(temp, temp.get(0), temp.get(temp.size()-1));
                    for(int k=0;k<arr.size();k++){
                        if(OCH_compare(true, newOCH, arr.get(k))){
                            flag[i] = true;
                            flag[j] = true;
                        }
                    }
                }
            }
        }

         */
        int res = 0;
        for(int i=0;i<arr.size();i++){
            if(!flag[i]) res++;
        }
        return res;
    }

    public boolean OCH_find_solution(OCH_route och_route, ArrayList<ArrayList<OCH_route>> OCH_links){
        for(int i=0;i<OCH_links.size();i++){
            for(int j=0;j<OCH_links.get(i).size();j++){
                if(OCH_compare(true, OCH_links.get(i).get(j), och_route))
                    return true;
            }
        }
        return false;
    }
    public boolean OCH_find_route(OCH_route och_route, ArrayList<OCH_route> OCH_link){
        for(int i=0;i<OCH_link.size();i++){
            if(OCH_compare(true, OCH_link.get(i), och_route))
                return true;
        }
        return false;
    }
    public boolean OCH_pair(OCH_route OCH1, OCH_route OCH2){
        ArrayList<Integer> s;
        ArrayList<Integer> l;
        if(OCH1.och.size() > OCH2.och.size()){
            s = new ArrayList<>(OCH2.och);
            l = new ArrayList<>(OCH1.och);
        }
        else{
            s = new ArrayList<>(OCH1.och);
            l = new ArrayList<>(OCH2.och);
        }
        if(s.get(0) > s.get(s.size()-1)) Collections.reverse(s);
        if(l.get(0) > l.get(l.size()-1)) Collections.reverse(l);
        boolean isPair = false;
        for(int i=0;i<l.size();i++){
            int flag = 1;

            if(l.get(i).equals(s.get(0))){
                if(l.size() - i - 1 < s.size() - 1) {
                    break;
                }
                boolean temp = true;
                for(int j=i+1;j<l.size();j++){
                    if(!l.get(j).equals(s.get(flag))){
                        temp = false;
                        break;
                    }
                    flag++;
                    if(flag >= s.size()) break;
                }
                if(temp) {
                    isPair = true;
                    break;
                }
            }
        }
        return isPair;
    }
    public boolean Wl_assign(int route_num, ArrayList<OCH_route> res, ArrayList<OMS_WL> OMS_Wavelength, Map<Integer, Integer> wl_assignment) {
        ArrayList<Integer> result = new ArrayList<>();
        ArrayList<Integer> st = new ArrayList<>();
        ArrayList<Integer> en = new ArrayList<>();
        for (OCH_route re : res) {
            for (int j = 0; j < re.och.size() - 1; j++) {
                st.add(re.och.get(j));
                en.add(re.och.get(j + 1));
            }
        }
        int[][] wl = new int[st.size()][instance.num_of_wavelength];
        ArrayList<ArrayList<Integer>> wls = new ArrayList<>();
        for(int i=0;i<wl.length;i++){
            for (OMS_WL oms_wl : OMS_Wavelength) {
                if (st.get(i) == oms_wl.start &&
                        en.get(i) == oms_wl.end) {
                    wls.add(new ArrayList<>(oms_wl.wavelength));
                    break;
                }
            }
        }
        for(int i=0;i<wls.size();i++){
            for(int j=0;j<wl[0].length;j++){
                if(wls.get(i).contains(instance.wavelength[j])) wl[i][j] = 1;
            }
        }
        try {
            for (int i = 0; i < wl[0].length; i++) {
                int f = 0;
                for (int[] ints : wl) {
                    f += ints[i];
                }
                if (f == wl.length) result.add(instance.wavelength[i]);
            }
        }catch (ArrayIndexOutOfBoundsException e){
            System.out.println();
        }
        if(result.size() == 0){
            System.out.format("%d号链路可用波长不足，无法完成分配", route_num);
            return false;
        }
        for(int i=0;i<st.size();i++){
            for(int j=0;j<OMS_Wavelength.size();j++){
                if((st.get(i) == OMS_Wavelength.get(j).start &&
                        en.get(i) == OMS_Wavelength.get(j).end) ||
                        (st.get(i) == OMS_Wavelength.get(j).end &&
                                en.get(i) == OMS_Wavelength.get(j).start)){
                    OMS_Wavelength.get(j).wavelength.remove(result.get(0));
                }
            }
        }
        wl_assignment.put(route_num, result.get(0));
        return true;
    }

    public void SolutionPrint(Solution solution){
        for(int i=0;i<solution.service_links.size();i++){
            for(int j=0;j<solution.OCH_links.get(i).size();j++){
                for(int k=0;k<solution.OCH_links.get(i).get(j).och.size();k++){
                    System.out.print(solution.OCH_links.get(i).get(j).och.get(k));
                    System.out.print(" ");
                }
                System.out.print("|");
            };
            System.out.println();
        }

        System.out.println(solution.cost);
    }

    public void MapClone(Map<ArrayList<Integer>, Integer> ori, Map<ArrayList<Integer>, Integer> des){
        for(ArrayList<Integer> arrayList : ori.keySet()){
            ArrayList<Integer> a = new ArrayList<>(arrayList);
            des.put(a, ori.get(arrayList));
        }
    }
}
