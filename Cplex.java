import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

public class Cplex{

    public static void main(String[] args) {
        try {
            IloCplex cplex = new IloCplex();
            Instance instance = new Instance(
                    "C:\\Users\\Rain\\Desktop\\graduation\\src\\examples\\uknet.txt");
            ArrayList<Integer> och_st = new ArrayList<>();
            ArrayList<Integer> och_en = new ArrayList<>();
            for(int i=0;i<instance.st.size();i++){
                och_st.add(instance.st.get(i));
                och_st.add(instance.en.get(i));
                och_en.add(instance.en.get(i));
                och_en.add(instance.st.get(i));
            }
            //ArrayList<Integer> st_point = new ArrayList<>();
            //ArrayList<Integer> en_point = new ArrayList<>();
            //ArrayList<Integer> mid_point = new ArrayList<>();
            ArrayList<Integer> req_st = new ArrayList<>();
            ArrayList<Integer> req_en = new ArrayList<>();
            for(int i=0;i<instance.requests.size();i++){
                req_st.add(instance.requests.get(i).start);
                req_en.add(instance.requests.get(i).end);
            }
            ArrayList<Integer> as_st = new ArrayList<>();//作为多少需求的起点
            ArrayList<Integer> as_en = new ArrayList<>();//作为多少需求的终点
            for(int i = 0; i < instance.nodes.size();i++){
                int as_st_num = 0;
                int as_en_num = 0;
                for(int j=0;j<instance.requests.size();j++){
                    if(instance.nodes.get(i) == instance.requests.get(j).start){
                        as_st_num++;
                    }
                    if(instance.nodes.get(i) == instance.requests.get(j).end){
                        as_en_num++;
                    }
                }
                as_st.add(as_st_num);
                as_en.add(as_en_num);
            }
            /*
            for(int i=0;i<instance.nodes.size();i++){
                for(int j=0;j<instance.num_of_requests;j++){
                    if(req_st.contains(instance.nodes.get(i)) && !st_point.contains(instance.nodes.get(i))){
                        st_point.add(i);
                    }
                    else if(req_en.contains(instance.nodes.get(i)) && !en_point.contains(instance.nodes.get(i))){
                        en_point.add(i);
                    }
                }
            }
            for(int i=0;i<instance.nodes.size();i++){
                if(!st_point.contains(instance.nodes.get(i)) && !en_point.contains(instance.nodes.get(i))) mid_point.add(instance.nodes.get(i));
            }
             */
            ArrayList<ArrayList<Integer>> kn_1 = new ArrayList<>();
            ArrayList<ArrayList<Integer>> kn_0 = new ArrayList<>();
            for(int i=0;i<instance.nodes.size();i++){
                ArrayList<Integer> temp1 = new ArrayList<>();
                ArrayList<Integer> temp2 = new ArrayList<>();
                for(int j=0;j<och_st.size();j++){
                    if(och_st.get(j).equals(instance.nodes.get(i))){
                        temp1.add(j);
                    }
                    if(och_en.get(j).equals(instance.nodes.get(i))){
                        temp2.add(j);
                    }
                }
                kn_1.add(temp1);
                kn_0.add(temp2);
            }

            IloIntVar[][] x_kj = new IloIntVar[och_en.size()][instance.requests.size()];
            for(int i=0;i<och_en.size();i++){
                x_kj[i] = cplex.intVarArray(instance.requests.size(), 0, 1);
            }
            IloIntVar[] y_k = cplex.intVarArray(och_st.size(), 0, 1);
            int[][] delta = new int[och_st.size()][instance.wavelength.length];
            for(int i=0;i<och_en.size();i++){
                for(int j=0;j<instance.OMS_Wavelength.size();j++){
                    if((och_st.get(i) == instance.OMS_Wavelength.get(j).start && och_en.get(i) ==
                            instance.OMS_Wavelength.get(j).end)||(och_en.get(i) == instance.OMS_Wavelength.get(j).start
                            && och_st.get(i) == instance.OMS_Wavelength.get(j).end)){
                        for(int k=0;k<instance.OMS_Wavelength.get(j).wavelength.size();k++){
                            delta[i][instance.OMS_Wavelength.get(j).wavelength.get(k)-1] = 1;
                        }
                    }
                }
            }
            IloIntVar[] p_lambda = cplex.intVarArray(instance.num_of_wavelength, 0, 1);
            for(int k=0;k< och_st.size();k++) {
                IloNumExpr n1 = cplex.numExpr();
                for (int j = 0; j < instance.requests.size(); j++) {
                    n1 = cplex.sum(n1, x_kj[k][j]);
                }
                cplex.addLe(n1, cplex.prod(y_k[k], 21474836));
            }
            IloNumExpr obj = cplex.numExpr();
            for(int i=0;i<och_st.size();i++){
                obj = cplex.sum(obj, y_k[i]);
            }
            cplex.addMinimize(obj);
            /*
            IloNumExpr[] n1 = new IloNumExpr[st_point.size()];
            for(int s=0;s<st_point.size();s++) {
                IloNumExpr n1_1 = cplex.numExpr();
                for (int j = 0; j < instance.num_of_requests; j++) {
                    for (int k = 0; k < kn_1.get(st_point.get(s)).size(); k++) {
                        n1_1 = cplex.sum(n1_1, x_kj[kn_1.get(st_point.get(s)).get(k)][j]);
                    }
                }
                cplex.addEq(n1_1, 1);
            }

            IloNumExpr n2 = cplex.numExpr();
            for(int e=0;e<en_point.size();e++) {
                IloNumExpr n2_1 = cplex.numExpr();
                for (int j = 0; j < instance.num_of_requests; j++) {
                    for (int k = 0; k < kn_0.get(en_point.get(e)).size(); k++) {
                        n2_1 = cplex.sum(n2_1, x_kj[kn_0.get(en_point.get(e)).get(k)][j]);
                    }
                }
                cplex.addEq(n2_1, 1);
            }
            */
            for(int j=0;j< instance.num_of_requests;j++){
                IloNumExpr n2_1 = cplex.numExpr();
                IloNumExpr n2_1_1 = cplex.numExpr();
                for(int k=0;k<kn_1.get(instance.requests.get(j).start).size();k++){
                    n2_1 = cplex.sum(n2_1, x_kj[kn_1.get(instance.requests.get(j).start).get(k)][j]);
                }
                for(int k=0;k<kn_0.get(instance.requests.get(j).start).size();k++){
                    n2_1_1 = cplex.sum(n2_1_1, x_kj[kn_0.get(instance.requests.get(j).start).get(k)][j]);
                }
                cplex.addEq(cplex.diff(n2_1, n2_1_1), 1);
                IloNumExpr n2_2 = cplex.numExpr();
                IloNumExpr n2_2_1 = cplex.numExpr();
                for(int k=0;k<kn_1.get(instance.requests.get(j).end).size();k++){
                    n2_2 = cplex.sum(n2_2, x_kj[kn_1.get(instance.requests.get(j).end).get(k)][j]);
                }
                for(int k=0;k<kn_0.get(instance.requests.get(j).end).size();k++){
                    n2_2_1 = cplex.sum(n2_2_1, x_kj[kn_0.get(instance.requests.get(j).end).get(k)][j]);
                }
                cplex.addEq(cplex.diff(n2_2, n2_2_1), -1);
                for(int n=0;n<instance.nodes.size();n++){
                    if((instance.nodes.get(n) == instance.requests.get(j).start)||
                            (instance.nodes.get(n) == instance.requests.get(j).end)){
                        continue;
                    }
                    IloNumExpr n2_3 = cplex.numExpr();
                    IloNumExpr n2_3_1 = cplex.numExpr();
                    for(int k=0;k<kn_1.get(instance.nodes.get(n)).size();k++){
                        n2_3 = cplex.sum(n2_3, x_kj[kn_1.get(instance.nodes.get(n)).get(k)][j]);
                    }
                    for(int k=0;k<kn_0.get(instance.nodes.get(n)).size();k++){
                        n2_3_1 = cplex.sum(n2_3_1, x_kj[kn_0.get(instance.nodes.get(n)).get(k)][j]);
                    }
                    cplex.addEq(cplex.diff(n2_3, n2_3_1), 0);
                }
            }
            /*
            IloNumExpr[] n3 = new IloNumExpr[instance.nodes.size()];
            for(int n=0;n<instance.nodes.size();n++) {
                IloNumExpr n3_1 = cplex.numExpr();
                IloNumExpr n3_2 = cplex.numExpr();
                for (int j = 0; j < instance.num_of_requests; j++) {
                    for (int k = 0; k < kn_1.get(instance.nodes.get(n)).size(); k++) {
                        n3_1 = cplex.sum(n3_1, x_kj[kn_1.get(instance.nodes.get(n)).get(k)][j]);
                    }
                }
                for (int j = 0; j < instance.num_of_requests; j++) {
                    for (int k = 0; k < kn_0.get(instance.nodes.get(n)).size(); k++) {
                        n3_1 = cplex.diff(n3_1, x_kj[kn_0.get(instance.nodes.get(n)).get(k)][j]);
                    }
                }
                cplex.addLe(n3_1, as_en.get(n)- as_st.get(n));
            }
            */
            IloNumExpr[][] n4 = new IloNumExpr[instance.num_of_wavelength][instance.requests.size()];
            for(int lam=0;lam<instance.num_of_wavelength;lam++){
                for(int j=0;j<instance.requests.size();j++){
                    IloNumExpr n4_1 = cplex.numExpr();
                    IloNumExpr n4_2 = cplex.numExpr();
                    for(int k=0;k<och_st.size();k++){
                        n4_1 = cplex.sum(n4_1, x_kj[k][j]);
                    }
                    IloNumExpr temp = cplex.prod(cplex.diff(p_lambda[lam], 1), 2147483647);
                    n4_1 = cplex.sum(n4_1, temp);
                    for(int k=0;k<och_st.size();k++){
                        n4_2 = cplex.sum(n4_2, cplex.prod(x_kj[k][j], delta[k][lam]));
                    }
                    cplex.addLe(n4_1, n4_2);

                }
            }
            IloNumExpr n5 = cplex.numExpr();
            for(int lam=0;lam<instance.num_of_wavelength;lam++){
                n5 = cplex.sum(n5, p_lambda[lam]);
            }
            cplex.addGe(n5, 1);
            for(int k=0;k< och_en.size();k++){
                IloNumExpr n6 = cplex.numExpr();
                for(int j=0;j< instance.num_of_requests;j++){
                    n6 = cplex.sum(n6, cplex.prod(x_kj[k][j], instance.requests.get(j).req));
                }
                cplex.addLe(n6, instance.bandwidth_limit);
            }
            if (cplex.solve()) {
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value = " + cplex.getObjValue());
            }
            cplex.exportModel("qwe.lp");
            for(int i=0;i< x_kj.length;i++){
                for(int j=0;j<x_kj[0].length;j++){
                    System.out.print(cplex.getValue(x_kj[i][j]));
                    System.out.print(" ");
                }
                System.out.println();
            }
            cplex.end();

        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
    }
}
