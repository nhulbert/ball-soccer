package game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import org.siprop.bullet.util.Vector3;

public class AIController {
    private ArrayList<Entity> entities;

    private double[] times;
    private double[] dists;
    public double debugDist = 0;
    private Integer[] entOrder;
    private static final float A = 35.21493f;
    private static final float K = 0.899277f;

    private static final float[] serverGoal = {159f,81f};
    private static final float[] AIGoal = {1f,81f};
    float[] serverEdge1 = {serverGoal[0], serverGoal[1]-30};
    float[] serverEdge2 = {serverGoal[0], serverGoal[1]+30};
    float[] AIEdge1 = {AIGoal[0], AIGoal[1]+30};
    float[] AIEdge2 = {AIGoal[0], AIGoal[1]-30};

    public AIController(ArrayList<Entity> entities){
        this.entities = entities;
        times = new double[entities.size()-2];
        dists = new double[entities.size()-2];
        entOrder = new Integer[entities.size()-2];
        for (int i=0; i<entities.size()-2; i++){
            entOrder[i] = i;
        }
    }

    public void update(){
        Entity b = entities.get(2);
        for (int i=0; i<entities.size()-2; i++){
            if (i != 2){
                Entity e = entities.get(i);

                double tempTime = 0,d,c;

                float[] vd;

                for (int h=0; h<3; h++) {
                    vd = new float[]{b.curPosX+b.curVelX*(float)tempTime - e.curPosX - e.curVelX*(float)tempTime,
                            b.curPosZ+b.curVelZ*(float)tempTime - e.curPosZ - e.curVelZ*(float)tempTime};

                    d = Math.sqrt(vd[0] * vd[0] + vd[1] * vd[1]);
                    if (h == 0) dists[i] = d;

                    vd[0] /= d;
                    vd[1] /= d;

                    float[] vv = new float[]{e.curVelX - b.curVelX, e.curVelZ - b.curVelZ};

                    c = (vv[0] * vd[0] + vv[1] * vd[1]) / A;

                    tempTime = Math.sqrt(Math.max(2 * (d - 5) / (K * A) + c * c, 0)) - c;
                    if (Double.isNaN(tempTime)){
                        System.out.print("hello");
                    }
                }
                times[i] = tempTime;
            }
        }

        Arrays.sort(entOrder,new DistsComparator());

        debugDist = times[1] - times[0];

        for (int i=0; i<entities.size()-2; i++) {
            if(i != 2) {
                Entity e = entities.get(i);
                if (e.isAI) {
                    int id = i;
                    if (id <2) id++;
                    boolean isServer = (id%2) == 1;

                    float[] ownGoal = isServer ? serverGoal : AIGoal;
                    float[] targGoal = isServer ? AIGoal : serverGoal;
                    float[] ownEdge1 = isServer ? serverEdge1 : AIEdge1;
                    float[] ownEdge2 = isServer ? serverEdge2 : AIEdge2;
                    float[] targEdge1 = isServer ? AIEdge1 : serverEdge1;
                    float[] targEdge2 = isServer ? AIEdge2 : serverEdge2;
                    
                    if (times[i] - times[entOrder[0]] < 0.2){
                        float[] bPos = {b.curPosX+b.curVelX*(float)times[i], b.curPosZ+b.curVelZ*(float)times[i]};
                        float[] ePos = {e.curPosX+e.curVelX*(float)times[i], e.curPosZ+e.curVelZ*(float)times[i]};
                        float[] v = {bPos[0]-ePos[0], bPos[1]-ePos[1]};

                        float[] ownVec1 = {ownEdge1[0]-bPos[0], ownEdge1[1]-bPos[1]};
                        float[] ownVec2 = {ownEdge2[0]-bPos[0], ownEdge2[1]-bPos[1]};

                        float[] targVec = {targGoal[0]-bPos[0], targGoal[1]-bPos[1]};

                        float d = (float)Math.sqrt(ownVec1[0]*ownVec1[0]+ownVec1[1]*ownVec1[1]);
                        ownVec1[0] /= d;
                        ownVec1[1] /= d;

                        d = (float)Math.sqrt(ownVec2[0]*ownVec2[0]+ownVec2[1]*ownVec2[1]);
                        ownVec2[0] /= d;
                        ownVec2[1] /= d;

                        Vector3 imp = new Vector3(v[0],0,v[1]);

                        if ((v[1]*ownVec1[0]-v[0]*ownVec1[1] > 0) && (v[1]*ownVec2[0]-v[0]*ownVec2[1] < 0)){
                            float test1 = ownVec1[0]*v[0]+ownVec1[1]*v[1];
                            float test2 = ownVec2[0]*v[0]+ownVec2[1]*v[1];
                            Vector3 toAdd = (test1 > test2) ? new Vector3(-v[1],0,v[0]) : new Vector3(v[1],0,-v[0]);
                            toAdd.scale(Math.max(0.1f,10/(float)dists[i]));
                            imp.add(imp,toAdd);
                        }
                        else {
                            d = (float)Math.sqrt(v[0]*v[0]+v[1]*v[1]);
                            float[] vn = {v[0]/d,v[1]/d};
                            d = (float)Math.sqrt(targVec[0]*targVec[0]+targVec[1]*targVec[1]);
                            targVec[0] /= d; targVec[1] /= d;
                            float dot = vn[0]*targVec[0]+vn[1]*targVec[1];
                            float dotPerp = v[1]*targVec[0]-v[0]*targVec[1];
                            if (dot < 0.5f && dists[i] > 8){
                                Vector3 toAdd = (dotPerp > 0) ? new Vector3(-v[1],0,v[0]) : new Vector3(v[1],0,-v[0]);
                                toAdd.scale(Math.min(10f,10f*Math.abs(dotPerp)/(float)dists[i]));
                                imp.add(toAdd, new Vector3(v[0],0,v[1]));
                            }
                            else{
                                float hitDist = Math.max(Math.min(4.2f+(dot-0.4f)/0.6f*4.2f,4.2f),0f);

                                imp = new Vector3((bPos[0] - hitDist*targVec[0])-ePos[0], 0, (bPos[1] - hitDist*targVec[1])-ePos[1]);
                            }
                        }

                        imp.normalize();
                        e.applyCentralImpulse(imp);

                        /*if (v.x != 0 || v.z != 0) {
                            v.normalize();
                            e.applyCentralImpulse(v);
                        }*/
                    }
                    else{
                        float[] targ={},v={};
                        double d =0,tempTime = 0;
                        float c=0;

                        for (int h=0; h<3; h++) {
                            targ = new float[]{(ownGoal[0] + b.curPosX+(float)tempTime*b.curVelX) / 2f, (ownGoal[1] + b.curPosZ+(float)tempTime*b.curVelZ) / 2f};

                            v = new float[]{targ[0] - e.curPosX - (float)tempTime*e.curVelX, targ[1] - e.curPosZ - (float)tempTime*e.curVelZ};
                            d = Math.sqrt(v[0] * v[0] + v[1] * v[1]);
                            v[0] /= d;
                            v[1] /= d;

                            c = b.curVelX * v[0] + b.curVelZ * v[1];

                            tempTime = Math.sqrt(Math.max(2 * (d) / (K * A) + (c/A) * (c/A), 0)) - (c/A);
                        }

                        if (A*tempTime > c-5){
                            e.applyCentralImpulse(new Vector3(v[0],0,v[1]));
                        }
                        else{
                            e.applyCentralImpulse(new Vector3(-v[0],0,-v[1]));
                        }
                    }
                }
            }
        }
    }

    private class DistsComparator implements Comparator<Integer>{
        public int compare(Integer a, Integer b){
            double v1 = (a == 2) ? Double.MAX_VALUE : times[a];
            double v2 = (b == 2) ? Double.MAX_VALUE : times[b];
            return Double.compare(v1,v2);
        }
    }
}
