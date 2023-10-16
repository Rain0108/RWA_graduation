import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class main {
    public static void main(String[] args) {
        Instance instance = new Instance(
                "D:\\graduation\\src\\examples\\panEUnet.txt");
        Operators operator = new Operators(instance);
        Solution solution = new Solution(operator, instance);
        boolean success = solution.init(operator, instance);
        //operator.OCH_pair(new OCH_route(a1,0,3), new OCH_route(a2,3,1));
        if(success) {
            Search search = new Search(instance, operator, solution);
            SA sa = new SA(instance, operator, solution, search);
            sa.SA_run();
        }
        //Solution s = sa.generate_new(solution);
        //operator.SolutionPrint(solution);
        //operator.SolutionPrint(s);
        //ArrayList<Integer> temp = new ArrayList<>();
        //temp.add(0);
        //temp.add(1);
        //search.search_solution(temp, 2,2); //pre,cur都是上限
        //search.route_search(solution, null, null, 1, 2, 3);

    }
}
