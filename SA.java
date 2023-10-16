import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class SA {
    public int iteration; //迭代次数
    public double alpha; //降温速率(0.8-0.99之间)
    public double T0; //初始温度
    public double Tf; //终止温度
    public double T;  //当前温度
    public int mP; //多粒子参数
    public Solution ini_Solution;
    public Solution cur_Solution;
    public Solution best_Solution;
    public Instance instance;
    public Operators operators;
    public Search search;
    public SA(Instance instance, Operators operators, Solution solution, Search search){
        this.instance = instance;
        this.operators = operators;
        this.ini_Solution = solution;
        this.cur_Solution = solution;
        this.best_Solution = solution;
        this.search = search;
        iteration = 100;
        alpha = 0.8;
        T0 = 300;
        Tf  =0.01;
        T = T0;
        mP = 1;
    }
    public Solution generate_new(Solution ini_solution){
        ArrayList<Integer> route_num = new ArrayList<>();
        HashSet<Integer> set = new HashSet<>();
        int change_num = operators.random.nextInt(ini_solution.OCH_links.size()-1)+1;
        randomSet(0, ini_solution.OCH_links.size()-1, change_num+1, set);
        Object[] route_num1 = set.toArray();
        for(int i=0;i<set.size();i++){
            route_num.add((Integer) route_num1[i]);
        }
        Collections.sort(route_num);
        int[] pre_size = new int[route_num.size()];
        int[] cur_size = new int[route_num.size()];
        for(int i=0;i<route_num.size();i++) {
            if (ini_solution.OCH_links.get(i).size() == 1) {
                pre_size[i] = 0;
                cur_size[i] = 0;
            } else {
                pre_size[i] = operators.random.nextInt(ini_solution.OCH_links.get(i).size()-1) + 1;
                cur_size[i] = operators.random.nextInt(ini_solution.OCH_links.get(i).size()-1) + 1;
            }
        }
        cur_Solution = search.search_solution(ini_solution, route_num, pre_size, cur_size);
        while(!cur_Solution.feasible){
            route_num = new ArrayList<>();
            set = new HashSet<>();
            change_num = operators.random.nextInt(ini_solution.OCH_links.size()-1)+1;
            randomSet(0, ini_solution.OCH_links.size()-1, change_num+1, set);
            route_num1 = set.toArray();
            for(int i=0;i<set.size();i++){
                route_num.add((Integer) route_num1[i]);
            }
            pre_size = new int[route_num.size()];
            cur_size = new int[route_num.size()];
            for(int i=0;i<route_num.size();i++){
                pre_size[i] = operators.random.nextInt(ini_solution.OCH_links.get(i).size())+1;
                cur_size[i] = operators.random.nextInt(ini_solution.OCH_links.get(i).size())+1;
            }
            cur_Solution = search.search_solution(cur_Solution, route_num, pre_size, cur_size);
        }
        return cur_Solution;
    }
    public boolean Metrospolis(Solution old_Solution, Solution new_Solution){
        if(!new_Solution.feasible) return false;
        double p = Math.exp((old_Solution.cost - new_Solution.cost) / T);
        return operators.random.nextDouble() < p;
    }
    public void SA_run(){
        ArrayList<Integer> costs = new ArrayList<>();
        costs.add(ini_Solution.cost);
        best_Solution = ini_Solution;
        while (this.T > this.Tf){
            for(int i=0;i<this.iteration;i++){
                Solution new_Solution = generate_new(best_Solution);
                if(new_Solution.feasible && new_Solution.cost < best_Solution.cost){
                    best_Solution = new_Solution;
                }
                else{
                    if(Metrospolis(best_Solution, new_Solution)){
                        best_Solution = new_Solution;
                    }
                }

            }
            operators.SolutionPrint(best_Solution);
            costs.add(best_Solution.cost);
            T = T * alpha;
        }
        System.out.println("--------------------完成搜索---------------------");
        operators.SolutionPrint(ini_Solution);
        operators.SolutionPrint(best_Solution);
        for(int i=0;i<costs.size()-1;i++){
            System.out.print(costs.get(i));
            System.out.print(", ");
        }
        System.out.print(costs.get(costs.size()-1));
        System.out.println();
    }
/*
    public void SA_run_adapted(){
        ArrayList<Integer> costs = new ArrayList<>();
        costs.add(ini_Solution.cost);
        best_Solution = ini_Solution;
        while (this.T > this.Tf) {
            for(int i=0;i<this.iteration;i++){
                Solution new_Solution = generate_new(best_Solution);
                for(int j=0;j<mP-1;j++){
                    Solution temp_Solution = generate_new(best_Solution);
                    if(temp_Solution.cost < new_Solution.cost){
                        new_Solution = temp_Solution;
                    }
                }
                //Solution new_Solution = generate_new(best_Solution);
                if (new_Solution.feasible && new_Solution.cost < best_Solution.cost) {
                    best_Solution = new_Solution;
                } else {
                    if (Metrospolis(best_Solution, new_Solution)) {
                        best_Solution = new_Solution;
                    }
                }
        }
            operators.SolutionPrint(best_Solution);
            costs.add(best_Solution.cost);
            T = T * alpha;
        }
        System.out.println("--------------------完成搜索---------------------");
        operators.SolutionPrint(ini_Solution);
        operators.SolutionPrint(best_Solution);
        for(int i=0;i<costs.size()-1;i++){
            System.out.print(costs.get(i));
            System.out.print(", ");
        }
        System.out.print(costs.get(costs.size()-1));
        System.out.println();
    }
*/

    public static void randomSet(int min, int max, int n, HashSet<Integer> set) {
        //生成指定范围内互不相同的随机数
        if (n > (max - min + 1) || max < min) return;
        for (int i = 0; i < n; i++) {
            int num = (int) (Math.random() * (max - min)) + min;
            set.add(num);
        }
        int setSize = set.size();
        if (setSize < n) {
            randomSet(min, max, n - setSize, set);
        }
    }

}
