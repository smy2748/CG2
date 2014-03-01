//
//  Rasterizer.java
//  
//
//  Created by Joe Geigel on 1/21/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

/**
 * 
 * This is a class that performas rasterization algorithms
 *
 */

import java.util.*;

public class Rasterizer {
    
    /**
     * number of scanlines
     */
    int n_scanlines;
    
    /**
     * Constructor
     *
     * @param n - number of scanlines
     *
     */
    Rasterizer (int n)
    {
        n_scanlines = n;
    }
    
    /**
     * Draw a filled polygon in the simpleCanvas C.
     *
     * The polygon has n distinct vertices. The 
     * coordinates of the vertices making up the polygon are stored in the 
     * x and y arrays.  The ith vertex will have coordinate  (x[i], y[i])
     *
     * You are to add the implementation here using only calls
	 * to C.setPixel()
     */
    public void drawPolygon(int n, int x[], int y[], simpleCanvas C)
    {
        int ymin=-1, ymax=-1;
        ArrayList<EdgeBucket> buckets = new ArrayList<EdgeBucket>();

        for(int i=0; i< n-1; i++){
            if(y[i] != y[i+1]){
                buckets.add(createEdgeBucket(x[i],y[i],x[i+1], y[i+1]));
                if(ymin > y[i] || ymin == -1){
                    ymin = y[i];
                }
                if(ymax < y[i] || ymax == -1){
                    ymax = y[i];
                }
            }
        }

        if(y[n-1] != y[0]){
            buckets.add(createEdgeBucket(x[n-1],y[n-1],x[0], y[0]));
            if(ymin > y[n-1] || ymin == -1){
                ymin = y[n-1];
            }
            if(ymax < y[n-1] || ymax == -1){
                ymax = y[n-1];
            }
        }

        EdgeTable eTable = new EdgeTable(ymax, ymin);

        for(EdgeBucket e : buckets){
            eTable.addEdgeBucket(e);
        }


        EdgeBucket first, last;
        int xStart, xEnd;
        ActiveEdgeList ael = new ActiveEdgeList();
        for(int scanLine=ymin; scanLine <= ymax; scanLine++){
            ael.add(eTable.getFirstEB(scanLine));
            ael.removePassedEdges(scanLine);
            ael.sort();
            for(int j = 0; j < ael.size()-1; j += 2){
                first = ael.get(j);
                last = ael.get(j+1);

                xStart = first.getXinit();
                if(first.getSum() > 0 && first.isNegative() == false){
                    xStart++;
                }

                xEnd = last.getXinit();
                if(last.getSum() > 0 && last.isNegative() == true){
                    xEnd --;
                }

                for (int xPix = xStart; xPix < xEnd; xPix++){
                    C.setPixel(xPix,scanLine);
                }

                first.updateSumAndX();
                last.updateSumAndX();


            }

        }

    }

    public EdgeBucket createEdgeBucket(int x1, int y1, int x2, int y2){
        int firstscan, ymax, xinit, dx, dy;
        boolean isNegative;
        if(y1 < y2){
            firstscan = y1;
            ymax = y2;
            xinit = x1;
        }
        else{
            firstscan = y2;
            ymax = y1;
            xinit=x2;
        }

        dy = y2-y1;
        dx = x2-x1;

        if((dy <0 && dx <=0) || (dy >0 && dx >= 0) ){
            isNegative = false;
        }
        else{
            isNegative = true;
        }

        if(dx <0){
            dx*= -1;
        }

        if(dy <0){
            dy *= -1;
        }

        return new EdgeBucket(firstscan,ymax,xinit,isNegative,dx,dy);

    }

    class ActiveEdgeList{
        protected ArrayList<EdgeBucket> list;

        public ActiveEdgeList(){
            list = new ArrayList<EdgeBucket>();
        }

        public void add(EdgeBucket e){
            EdgeBucket cur = e;

            while (cur != null){
                list.add(cur);
                cur = cur.getNext();
            }
        }

        public void removePassedEdges(int y){
            for(int i = 0; i< list.size(); i++){
                if(list.get(i).getYmax() <= y){
                    list.remove(i);
                    i--;
                }
            }
        }

        public void sort(){
            Collections.sort(list, new EdgeBucketComparator());
        }

        public EdgeBucket get(int index){
            return list.get(index);
        }

        public int size(){
            return list.size();
        }

    }

    class EdgeTable{
        protected EdgeBucket[] table;
        protected int ymin;

        public EdgeTable(int ymax, int ymin){
            table = new EdgeBucket[1+ymax-ymin];
            this.ymin = ymin;
        }

        public void addEdgeBucket(EdgeBucket e){
            int tIndex = e.getFirtscan()- ymin;
            if(table[tIndex] == null){
                table[tIndex] = e;
            }
            else{
                e.setNext(table[tIndex]);
                table[tIndex] = e;
            }

        }

        public EdgeBucket getFirstEB(int y){
            int tIndex = y - ymin;
            return table[tIndex];
        }
    }


    class EdgeBucket {
        protected int firtscan;
        protected int ymax;
        protected int xinit;
        protected boolean isNegative;
        protected int dx;
        protected int dy;
        protected int sum;
        protected EdgeBucket next;

        public EdgeBucket(int firstscan, int ymax, int xinit, boolean isNegative, int dx, int dy){
            this.firtscan = firstscan;
            this.ymax = ymax;
            this.xinit = xinit;
            this.isNegative = isNegative;
            this.dx =dx;
            this.dy = dy;
            this.sum = 0;
        }

        public void updateSumAndX(){
            sum += dx;
            if(dx != 0 && sum > dy){
                while (sum > dy){
                    sum -=dy;
                    if(isNegative){
                        xinit--;
                    }else{
                        xinit++;
                    }
                }
            }
        }

        public int getSum() {
            return sum;
        }

        public void setSum(int sum) {
            this.sum = sum;
        }

        public EdgeBucket getNext() {
            return next;
        }

        public void setNext(EdgeBucket next) {
            this.next = next;
        }

        public int getFirtscan() {
            return firtscan;
        }

        public void setFirtscan(int firtscan) {
            this.firtscan = firtscan;
        }

        public int getYmax() {
            return ymax;
        }

        public void setYmax(int ymax) {
            this.ymax = ymax;
        }

        public int getXinit() {
            return xinit;
        }

        public void setXinit(int xinit) {
            this.xinit = xinit;
        }

        public boolean isNegative() {
            return isNegative;
        }

        public void setNegative(boolean isNegative) {
            this.isNegative = isNegative;
        }

        public int getDx() {
            return dx;
        }

        public void setDx(int dx) {
            this.dx = dx;
        }

        public int getDy() {
            return dy;
        }

        public void setDy(int dy) {
            this.dy = dy;
        }

    }

    class EdgeBucketComparator implements Comparator<EdgeBucket>{

        @Override
        public int compare(EdgeBucket o1, EdgeBucket o2) {
            if (o1.getXinit() != o2.getXinit()){
                Integer x1, x2;
                x1 = o1.getXinit();
                x2 = o2.getXinit();
                return x1.compareTo(x2);
            }

            Float mInv1 = ((float)o1.getDx()/(float)o1.getDy());
            Float mInv2 = ((float)o2.getDx()/(float)o2.getDy());
            return mInv1.compareTo(mInv2);
        }
    }
    
}
