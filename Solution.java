import java.util.*;

public class Solution {
    //解的形式：OCH_links包含所有需要建立的OCH链路，之后是为每个需求建立的业务链路
    //生成一个解之前需要初始化所有路径的剩余带宽
    public Operators operators;
    public Instance instance;
    public boolean feasible;
    public boolean wl_assign_right;
    public int cost;  //解的消耗，即为需要建立的OCH总数
    public ArrayList<ArrayList<OCH_route>> OCH_links;
    public ArrayList<OCH_route> service_links;
    public ArrayList<OCH_route> all_OCH;
    public Map<ArrayList<Integer>, Integer> res_bandwidth;  //当前每条边剩余的带宽容量
    public Map<Integer, Integer> wl_assignment; //当前解的波长分配方案
    public ArrayList<OMS_WL> OMS_Wavelength; //当前每条边可用的波长集合，在确定一条业务链之后更新
    public int och_max_iteration;  //在当前可用OCH中寻找不重复OCH的最大次数，超出则说明找不到下一个不重复的OCH
    public Solution(Operators operators, Instance instance){
        this.instance=instance;
        this.operators=operators;
        OCH_links = new ArrayList<>();
        service_links = new ArrayList<>();
        res_bandwidth = new HashMap<>(instance.res_bandwidth);
        cost = 0;
        och_max_iteration = 50;
        all_OCH = new ArrayList<>();
        wl_assignment = new HashMap<>();
        wl_assign_right = true;
        OMS_Wavelength = new ArrayList<>();
        for(int i=0;i<instance.OMS_Wavelength.size();i++){
            OMS_Wavelength.add(new OMS_WL(instance.OMS_Wavelength.get(i).start, instance.OMS_Wavelength.get(i).end,
                    new ArrayList<>(instance.OMS_Wavelength.get(i).wavelength)));
        }
    }

    public boolean init(Operators operators, Instance instance){
        //生成初始解
        boolean success = true;
        all_OCH = operators.get_all_OCH(operators, instance);
        //ArrayList<OCH_route> example = OCH_build(all_OCH, new Request(0, 11, 10));
        int x = 1;
        for(int i=0;i<instance.num_of_requests;i++){
            ArrayList<OCH_route> och_routes = OCH_build(this, all_OCH, i);
            if(och_routes == null){
                System.out.println("无法生成初始解，程序结束");
                return false;
            }
            OCH_links.add(och_routes);
            System.out.format("已满足第%d个请求", x++);
            System.out.println();
            ArrayList<Integer> nodes = new ArrayList<>();
            for (OCH_route och_route : och_routes) {
                for (int j = 0; j < och_route.och.size() - 1; j++) {
                    nodes.add(och_route.och.get(j));
                }
            }
            nodes.add(instance.requests.get(i).end);
            service_links.add(new OCH_route(nodes, instance.requests.get(i).start, instance.requests.get(i).end));
        }
        cost = operators.getCost(this);
        feasible = operators.isFeasible(this, instance);
        return true;
    }

    public ArrayList<OCH_route> OCH_build(Solution solution, ArrayList<OCH_route> all_OCH, int num){
        //输入一个需求，返回一条由OCH构成的链路，每条OMS都有带宽上限
        boolean[] v = new boolean[all_OCH.size()];
        ArrayList<OCH_route> res = new ArrayList<>();
        int cur_st = instance.requests.get(num).start;
        while(cur_st != instance.requests.get(num).end){
            ArrayList<OCH_route> cur_OCH = new ArrayList<>();
            for (int i=0;i<all_OCH.size();i++) {
                if (all_OCH.get(i).head == cur_st && !operators.isBeyond(instance.requests.get(num).end, all_OCH.get(i)) &&
                        !isOverload(res_bandwidth, all_OCH.get(i), instance.requests.get(num)) && !v[i] && new_OCH_WL_feasible(res, OMS_Wavelength, all_OCH.get(i)))
                    cur_OCH.add(all_OCH.get(i));
                //备选OCH的条件：1.能与现有路径接上，2.接上之后不会超过终点，3.没有超过带宽上限
                //4.与之前的路径没有重复边（这里不限制，在搜索过程中保证），5.与之前路径有公共波长
            }
            if(cur_OCH.size() == 0) {
                if(res.size() == 0){
                    System.out.println("不存在可用链路，跳过此次循环");
                    break;
                }
                load_change(res_bandwidth, false, res.get(res.size() - 1), instance.requests.get(num));
                System.out.print("没有后续链路，删除OCH：");
                for (int i = 0; i < all_OCH.size(); i++) {
                    if (operators.OCH_compare(false, all_OCH.get(i), res.get(res.size() - 1))) {
                        v[i] = true;
                        break;
                    }
                }
                res.remove(res.size() - 1);
                if(res.size() == 0){
                    cur_st = instance.requests.get(num).start;
                }
                else {
                    cur_st = res.get(res.size() - 1).tail;
                }
                continue;
            }
            int cur = -1;
            /*
            原先采取在cur_OCH中随机取路径，现在采用遍历
            try {
                cur = operators.random.nextInt(cur_OCH.size());
            }catch (IllegalArgumentException e){
                System.out.println();
            }
            int times = 0;
            boolean flag1 = false;
            while(operators.isRepeat(res, cur_OCH.get(cur))) {
                cur = operators.random.nextInt(cur_OCH.size());
                times++;
                if(times > och_max_iteration) {
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
                load_change(res_bandwidth, false, res.get(res.size()-1), instance.requests.get(num));
                System.out.print("无法搜到不重复路径，删除OCH：");
                for(Integer i : res.get(res.size()-1).och){
                    System.out.print(i);
                    System.out.print(' ');
                }
                System.out.println();

                for(int i=0;i<all_OCH.size();i++){
                    if(operators.OCH_compare(false, all_OCH.get(i), res.get(res.size()-1))){
                        v[i] = true;
                        break;
                    }
                }

                res.remove(res.size()-1);
                if(res.size() == 0) cur_st = instance.requests.get(num).start;
                else {
                    cur_st = res.get(res.size() - 1).tail;
                }
            }
            else {
                res.add(cur_OCH.get(cur));
                System.out.print("添加OCH：");
                for(Integer i : cur_OCH.get(cur).och){
                    System.out.print(i);
                    System.out.print(' ');
                }
                System.out.println();
                for(int i=0;i<all_OCH.size();i++){
                    if(operators.OCH_compare(false, all_OCH.get(i), res.get(res.size()-1))){
                        v[i] = true;
                        break;
                    }
                }

                load_change(res_bandwidth, true, res.get(res.size()-1), instance.requests.get(num));
                cur_st = res.get(res.size() - 1).tail;
            }
        }
        if(res.size() == 0) return null;
        //分配波长
        solution.wl_assign_right = operators.Wl_assign(num, res, OMS_Wavelength, wl_assignment);
        return res;
    }

    public boolean new_OCH_WL_feasible(ArrayList<OCH_route> res, ArrayList<OMS_WL> OMS_Wavelength, OCH_route och_route) {
        //判断新接上的och与现有的och是否有公共可用波长
        if(res.size() == 0) return true;
        ArrayList<Integer> st = new ArrayList<>();
        ArrayList<Integer> en = new ArrayList<>();
        for (OCH_route re : res) {
            for (int j = 0; j < re.och.size() - 1; j++) {
                st.add(re.och.get(j));
                en.add(re.och.get(j + 1));
            }
        }
        int flag = 0;
        for(int i=0;i<och_route.och.size()-1;i++){
            st.add(och_route.och.get(i));
            en.add(och_route.och.get(i+1));
            flag++;
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
        for(int i=0;i<wl[0].length;i++){
            int f = 0;
            for (int[] ints : wl) {
                f += ints[i];
            }
            if(f == wl.length) return true;
        }
        return false;
    }

    public boolean isOverload(Map<ArrayList<Integer>, Integer> bandwidth, OCH_route route, Request request){
        ArrayList<ArrayList<Integer>> routes = new ArrayList<>();
        for(int i=0;i<route.och.size()-1;i++){
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(route.och.get(i));
            temp.add(route.och.get(i+1));
            routes.add(temp);
        }
        try {
            for (ArrayList<Integer> integers : routes) {
                Collections.sort(integers);
                if (bandwidth.get(integers) < request.req) return true;
            }
        }catch (NullPointerException e){
            System.out.println("出现故障");
        }
        return false;
    }

    public void load_change(Map<ArrayList<Integer>, Integer> bandwidth, boolean isPlus, OCH_route route, Request request){
        //isplus为true则减少剩余载重
        ArrayList<ArrayList<Integer>> routes = new ArrayList<>();
        for(int i=0;i<route.och.size()-1;i++){
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(route.och.get(i));
            temp.add(route.och.get(i+1));
            routes.add(temp);
        }
        if(isPlus){
            for (ArrayList<Integer> integers : routes) {
                Collections.sort(integers);
                bandwidth.put(integers, bandwidth.get(integers) - request.req);
            }
        }
        else{
            for (ArrayList<Integer> integers : routes) {
                Collections.sort(integers);
                bandwidth.put(integers, bandwidth.get(integers) + request.req);
            }
        }
    }

}
class Special_node{
    int num;
    int layer;
    int previous;
    public Special_node(int num, int layer, int previous){
        this.layer = layer;
        this.num = num;
        this.previous = previous;
    }
}

