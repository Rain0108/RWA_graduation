import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Search {
    public Instance instance;
    public Operators operators;
    public Solution ini_solution;
    public Map<ArrayList<Integer>, Integer> res_bandwidth;
    public Search(Instance instance, Operators operators, Solution solution){
        this.instance = instance;
        this.operators = operators;
        this.ini_solution = solution;
        this.res_bandwidth = new HashMap<>(solution.res_bandwidth);
    }
    //存在问题：1.中间路段可能到达死角无法通过不重复路径到终点
    //2.中间路段需要避免经过原先路段两边的点
    public Solution search_solution(Solution ini_solution, ArrayList<Integer> route_num, int[] pre_size, int[] cur_size){
        //对需要变换的每条路径都随机删去其中几个连续的OCH，再使用route_search函数找出替代的OCH
        //pre_size，cur_size分别规定截下的OCH数量,新接上的OCH数量上限
        int[] OCH_use_times = new int[ini_solution.all_OCH.size()];
        for(int i=0;i<ini_solution.all_OCH.size();i++){
            if(operators.OCH_find_solution(ini_solution.all_OCH.get(i), ini_solution.OCH_links)){
                OCH_use_times[i]++;
            }
        }
        Solution newSolution = new Solution(operators, instance);
        newSolution.OMS_Wavelength = new ArrayList<>();
        for(int i=0;i<instance.OMS_Wavelength.size();i++){
            newSolution.OMS_Wavelength.add(new OMS_WL(ini_solution.OMS_Wavelength.get(i).start, ini_solution.OMS_Wavelength.get(i).end,
                    new ArrayList<>(instance.OMS_Wavelength.get(i).wavelength)));
        }
        newSolution.wl_assignment = new HashMap<>(ini_solution.wl_assignment);
        int count = 0;
        for(int i=0;i<ini_solution.service_links.size();i++){
            if(!route_num.contains(i)) {
                newSolution.OCH_links.add(ini_solution.OCH_links.get(i));
                boolean wl_assign = operators.Wl_assign(i, newSolution.OCH_links.get(i),
                        newSolution.OMS_Wavelength, newSolution.wl_assignment);
                if(!wl_assign) {
                    newSolution.wl_assign_right = false;
                    break;
                }
                continue;
            }
            if(ini_solution.OCH_links.get(i).size() <= pre_size[count] || pre_size[count] == 0 || cur_size[count] == 0) {
                newSolution.OCH_links.add(ini_solution.OCH_links.get(i));
                boolean wl_assign = operators.Wl_assign(i, newSolution.OCH_links.get(i),
                        newSolution.OMS_Wavelength, newSolution.wl_assignment);
                if(!wl_assign) {
                    newSolution.wl_assign_right = false;
                    break;
                }
                //System.out.println("-------------------------------------------");
                //System.out.format("%d号路径OCH数量不足，无法变换", i);
                //System.out.println();
                //System.out.println("-------------------------------------------");
                continue;
            }
            int start = operators.random.nextInt(ini_solution.OCH_links.get(i).size() - pre_size[count]);
            //System.out.format("开始为第%d条路径重新规划链路，起点为%d，终点为%d", i, ini_solution.OCH_links.get(i).get(start).head, ini_solution.OCH_links.get(i).get(start+pre_size[count]-1).tail);
            //System.out.println();

            ArrayList<OCH_route> new_links = route_search(ini_solution, newSolution, i, instance.requests.get(i), start, pre_size[count], cur_size[count], OCH_use_times);
            //System.out.println("-------------------------------------------");
            if(new_links != null) {
                newSolution.OCH_links.add(new_links);
                boolean wl_assign = operators.Wl_assign(i, new_links, newSolution.OMS_Wavelength, newSolution.wl_assignment);
                if(!wl_assign) {
                    newSolution.wl_assign_right = false;
                    break;
                }
                /*
                System.out.print("原先的路段：");
                //WL_setFree(ini_solution, i, newSolution.wl_assignment, newSolution.OMS_Wavelength);
                for (int j = 0; j < ini_solution.OCH_links.get(i).size(); j++) {
                    for (int k = 0; k < ini_solution.OCH_links.get(i).get(j).och.size(); k++) {
                        System.out.print(ini_solution.OCH_links.get(i).get(j).och.get(k));
                        System.out.print(" ");
                    }
                    if (j < ini_solution.OCH_links.get(i).size() - 1)
                        System.out.print("|");
                }
                System.out.println();
                System.out.print("需要调整的路段：");
                System.out.print(ini_solution.OCH_links.get(i).get(start).head);
                System.out.print("---");
                System.out.print(ini_solution.OCH_links.get(i).get(start + pre_size[count] - 1).tail);
                System.out.println();
                System.out.print("调整后路段：");
                for (int j = 0; j < new_links.size(); j++) {
                    for (int k = 0; k < new_links.get(j).och.size(); k++) {
                        System.out.print(new_links.get(j).och.get(k));
                        System.out.print(" ");
                    }
                    if (j < new_links.size() - 1)
                        System.out.print('|');
                }

                 */
            }
            else{
                newSolution.OCH_links.add(ini_solution.OCH_links.get(i));
                boolean wl_assign = operators.Wl_assign(i, ini_solution.OCH_links.get(i),
                        newSolution.OMS_Wavelength, newSolution.wl_assignment);
                if(!wl_assign) {
                    newSolution.wl_assign_right = false;
                    break;
                }
                //System.out.format("%d号路径不存在可替换链路", i);
            }
            count++;
        }
        for(int i=0;i<newSolution.OCH_links.size();i++){
            ArrayList<Integer> nodes = new ArrayList<>();

            for(int j=0;j<newSolution.OCH_links.get(i).size();j++){
                nodes.addAll(newSolution.OCH_links.get(i).get(j).och);
                nodes.remove(nodes.size()-1);
            }
            nodes.add(newSolution.OCH_links.get(i).get(newSolution.OCH_links.get(i).size()-1).tail);
            newSolution.service_links.add(new OCH_route(nodes, instance.requests.get(i).start, instance.requests.get(i).end));

        }
        newSolution.res_bandwidth = res_bandwidth;
        newSolution.all_OCH = ini_solution.all_OCH;
        newSolution.feasible = operators.isFeasible(newSolution, instance);
        newSolution.cost = operators.getCost(newSolution);
        return newSolution;
    }
    public ArrayList<OCH_route> route_search(Solution solution, Solution newSolution, int route_num, Request request,
                                             int start, int pre_size, int och_limit, int[] OCH_use_times){
        //在给定两点间寻找由OCH组成的最短路径，och_limit不为无穷大时不允许超过上限的och数量
        //搜索新路径前先释放旧路径占用的带宽
        ArrayList<OCH_route> pre_OCH = new ArrayList<>();
        boolean isUnhand = false;
        //int pre_size = operators.random.nextInt(pre_size_limit) + 1;
        ArrayList<OCH_route> result_unSort = new ArrayList<>();
        for(int j=0;j<start;j++) result_unSort.add(solution.OCH_links.get(route_num).get(j));
        for(int j=start+pre_size;j<solution.OCH_links.get(route_num).size();j++) result_unSort.add(solution.OCH_links.get(route_num).get(j));
        for (int j = start; j < start + pre_size; j++) pre_OCH.add(solution.OCH_links.get(route_num).get(j));

        for(int i=0;i<pre_OCH.size();i++){
            solution.load_change(res_bandwidth, false, pre_OCH.get(i), request);
        }
        for(int i=0;i<solution.all_OCH.size();i++){
            if(operators.OCH_find_route(solution.all_OCH.get(i), pre_OCH)){
                OCH_use_times[i]--;
            }
        }
        int start_node = solution.OCH_links.get(route_num).get(start).head;
        int end_node = solution.OCH_links.get(route_num).get(start+pre_size-1).tail;
        boolean[] v = new boolean[solution.all_OCH.size()];
        ArrayList<OCH_route> res = new ArrayList<>();
        int st = start_node;
        while (start_node != end_node){
            ArrayList<OCH_route> cur_OCH = new ArrayList<>();
            for(int i=0;i<solution.all_OCH.size();i++){
                if(OCH_use_times[i] == 0) continue;
                if(solution.all_OCH.get(i).head == start_node && !operators.isBeyond(end_node, solution.all_OCH.get(i)) &&
                        !solution.isOverload(res_bandwidth, solution.all_OCH.get(i), request) && !v[i] &&
                        solution.new_OCH_WL_feasible(result_unSort, newSolution.OMS_Wavelength, solution.all_OCH.get(i)) && res.size() < och_limit &&
                        !operators.isBeyond(request.end, solution.all_OCH.get(i))){
                    if(isReVisit(solution, route_num, solution.all_OCH.get(i), start, pre_size)) continue;
                    boolean f = false;
                    for (OCH_route pre_och : pre_OCH) {
                        if (operators.OCH_compare(false, pre_och, solution.all_OCH.get(i))) {
                            f = true;
                            break;
                        }
                    }
                    if(!f) cur_OCH.add(solution.all_OCH.get(i));
                }
            }
            if(cur_OCH.size() == 0) {
                for (int i = 0; i < solution.all_OCH.size(); i++) {
                    if(OCH_use_times[i] == 1) continue;
                    if (solution.all_OCH.get(i).head == start_node && !operators.isBeyond(end_node, solution.all_OCH.get(i)) &&
                            !solution.isOverload(res_bandwidth, solution.all_OCH.get(i), request) && !v[i] &&
                            solution.new_OCH_WL_feasible(result_unSort, newSolution.OMS_Wavelength, solution.all_OCH.get(i)) && res.size() < och_limit &&
                            !operators.isBeyond(request.end, solution.all_OCH.get(i))) {
                        if (isReVisit(solution, route_num, solution.all_OCH.get(i), start, pre_size)) continue;
                        boolean f = false;
                        for (OCH_route pre_och : pre_OCH) {
                            if (operators.OCH_compare(false, pre_och, solution.all_OCH.get(i))) {
                                f = true;
                                break;
                            }
                        }
                        if (!f) cur_OCH.add(solution.all_OCH.get(i));
                    }
                }
            }
            if(cur_OCH.size() == 0){
                if(res.size() == 0){
                    //System.out.println("不存在可替换链路，跳过此次循环");
                    for(int i=0;i<pre_OCH.size();i++){
                        solution.load_change(res_bandwidth, true, pre_OCH.get(i), request);
                    }
                    isUnhand = true;
                    break;
                }
                else {
                    solution.load_change(res_bandwidth, false, res.get(res.size() - 1), request);
                    for (int i = 0; i < solution.all_OCH.size(); i++) {
                        if (operators.OCH_compare(false, solution.all_OCH.get(i), res.get(res.size() - 1))) {
                            v[i] = true;
                            break;
                        }
                    }
                    res.remove(res.size() - 1);
                    result_unSort.remove(result_unSort.size() - 1);
                    if(res.size() == 0){
                        start_node = st;
                    }
                    else {
                        start_node = res.get(res.size() - 1).tail;
                    }

                }
                continue;
            }
            int cur = -1;
            /*
            int cur = operators.random.nextInt(cur_OCH.size());
            int times = 0;
            boolean flag1 = false;
            while(operators.isRepeat(res, cur_OCH.get(cur))) {
                cur = operators.random.nextInt(cur_OCH.size());
                times++;
                if(times > solution.och_max_iteration) {
                    flag1 = true;
                    break;
                }
            }
             */
            for(int i=0;i<cur_OCH.size();i++){
                if(!operators.isRepeat(res, cur_OCH.get(i))){
                    cur = i;
                    break;
                }
            }
            if(cur == -1) {
                solution.load_change(res_bandwidth, false, res.get(res.size()-1), request);
                for(int i=0;i<solution.all_OCH.size();i++){
                    if(operators.OCH_compare(false, solution.all_OCH.get(i), res.get(res.size()-1))){
                        v[i] = true;
                        break;
                    }
                }

                res.remove(res.size()-1);
                result_unSort.remove(result_unSort.size()-1);
                if(res.size() == 0) start_node = st;
                else {
                    start_node = res.get(res.size() - 1).tail;
                }
            }
            else {
                res.add(cur_OCH.get(cur));
                result_unSort.add(cur_OCH.get(cur));
                for(int i=0;i<solution.all_OCH.size();i++){
                    if(operators.OCH_compare(false, solution.all_OCH.get(i), res.get(res.size()-1))){
                        v[i] = true;
                        break;
                    }
                }

                solution.load_change(res_bandwidth, true, res.get(res.size()-1), request);
                start_node = res.get(res.size() - 1).tail;
            }
        }

        if(!isUnhand) {
            for(int i=0;i<solution.all_OCH.size();i++){
                if(operators.OCH_find_route(solution.all_OCH.get(i), res)){
                    OCH_use_times[i]++;
                }
            }
            for (int i = start - 1; i >= 0; i--) {
                res.add(0, solution.OCH_links.get(route_num).get(i));
            }
            for (int i = start + pre_size; i < solution.OCH_links.get(route_num).size(); i++) {
                res.add(solution.OCH_links.get(route_num).get(i));
            }
            return res;
        }
        for(int i=0;i<solution.all_OCH.size();i++){
            if(operators.OCH_find_route(solution.all_OCH.get(i), pre_OCH)){
                OCH_use_times[i]++;
            }
        }
        return null;
    }
    public boolean isReVisit(Solution solution, int route_num, OCH_route cur_OCH, int start, int pre_size){
        //判断新建立的och是否经过了两端已经过的点
        ArrayList<Integer> nodes = new ArrayList<>();
        for(int i=0;i<start;i++) {
            nodes.addAll(solution.OCH_links.get(route_num).get(i).och);
        }
        if(nodes.size() > 0) nodes.remove(nodes.size()-1);
        int temp_size = nodes.size();
        for(int i=start+pre_size;i<solution.OCH_links.get(route_num).size();i++)
            nodes.addAll(solution.OCH_links.get(route_num).get(i).och);
        if(nodes.size() == 0) return false;
        nodes.remove(temp_size);


        for(int i=0;i<cur_OCH.och.size();i++){
            for (Integer node : nodes) {
                if (cur_OCH.och.get(i).equals(node)) return true;
            }
        }
        return false;
    }

    public void WL_setFree(Solution solution, int route_num, Map<Integer, Integer> wl_assignment, ArrayList<OMS_WL> OMS_Wavelength){
        //释放整条路之前占用的波长
        ArrayList<Integer> temp_serviceLink = new ArrayList<>();
        for(int i=0;i<solution.OCH_links.get(route_num).size();i++){
            temp_serviceLink.addAll(solution.OCH_links.get(route_num).get(i).och);
        }
        ArrayList<Integer> st = new ArrayList<>();
        ArrayList<Integer> en = new ArrayList<>();
        for(int i=0;i<temp_serviceLink.size()-1;i++){
            st.add(temp_serviceLink.get(i));
            en.add(temp_serviceLink.get(i+1));
        }
        for(int i=0;i<st.size();i++){
            for(int j=0;j<OMS_Wavelength.size();j++){
                if((st.get(i) == OMS_Wavelength.get(j).start &&
                        en.get(i) == OMS_Wavelength.get(j).end) ||
                        (st.get(i) == OMS_Wavelength.get(j).end &&
                                en.get(i) == OMS_Wavelength.get(j).start)){
                    OMS_Wavelength.get(j).wavelength.add(wl_assignment.get(route_num));
                }
            }
        }
        wl_assignment.put(route_num, -1);
    }
}
