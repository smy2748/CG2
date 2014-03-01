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
     *
     * Author: Stephen Yingling
     */
    public void drawPolygon(int n, int x[], int y[], simpleCanvas C)
    {
        int ymin=n_scanlines, ymax=0;
        ArrayList<EdgeBucket> buckets = new ArrayList<EdgeBucket>();    //A list to hold the created buckets

        //Create EdgeBuckets for all pairs n, n+1
        for(int i=0; i< n-1; i++){
            if(y[i] != y[i+1]){
                buckets.add(createEdgeBucket(x[i],y[i],x[i+1], y[i+1]));

                //find the yMin and yMax values for this polygon
                if(ymin > y[i]){
                    ymin = y[i];
                }
                if(ymax < y[i]){
                    ymax = y[i];
                }
            }
        }

        //Create EdgeBucket for the edge connecting the last and first points.
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

        //Add each EdgeBucket to the EdgeTable
        for(EdgeBucket e : buckets){
            eTable.addEdgeBucket(e);
        }


        EdgeBucket first, last;
        int xStart, xEnd;

        ActiveEdgeList ael = new ActiveEdgeList();  //Initialize an empty ActiveEdgeList

        //Iterate from the lowest to highest scanline
        for(int scanLine=ymin; scanLine <= ymax; scanLine++){

            //Add all edgebuckets from the edgetable that begin at this scanline
            ael.add(eTable.getFirstEB(scanLine));

            //Remove all EdgeBuckets with ymax <= the current scanline
            ael.removePassedEdges(scanLine);

            //Sort the edgebuckets
            ael.sort();

            //Iterate over each pair in the active edge list
            for(int j = 0; j < ael.size()-1; j += 2){
                first = ael.get(j);
                last = ael.get(j+1);

                xStart = first.getxVal();

                //Round up for the exterior to interior crossing
                if(first.getSum() > 0 && first.isNegative() == false){
                    xStart++;
                }

                //The interior to exterior crossing pixel is either
                //1. Excluded if sum == 0 or
                //2. Rounded down if sum != 0
                xEnd = last.getxVal() - 1;

                //Set all pixels from xStart to xEnd
                for (int xPix = xStart; xPix <= xEnd; xPix++){
                    C.setPixel(xPix,scanLine);
                }

                //Update the sum and x values
                first.updateSumAndX();
                last.updateSumAndX();


            }

        }

    }

    /**
     * Creates an edgebucket from the given coordinates
     * @param x1 The x part of Point 1
     * @param y1 The y part of Point 1
     * @param x2 The x part of Point 2
     * @param y2 The y part of Point 2
     * @return The EdgeBucket representing the edge connecting the two points
     *
     * Author: Stephen Yingling
     */
    public EdgeBucket createEdgeBucket(int x1, int y1, int x2, int y2){
        int firstscan, ymax, xinit, dx, dy;
        boolean isNegative;

        //Determine the first scanline the edge is encountered,
        //the last scanline the edge is encountered, and
        //the initial x value for this edge.
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

        //Determine dx and dy
        dy = y2-y1;
        dx = x2-x1;

        //Determine whether the edge has a positive or negative inverse slope
        if((dy <0 && dx <=0) || (dy >0 && dx >= 0) ){
            isNegative = false;
        }
        else{
            isNegative = true;
        }

        //Make sure dx and dy are positive
        if(dx <0){
            dx*= -1;
        }

        if(dy <0){
            dy *= -1;
        }

        return new EdgeBucket(firstscan,ymax,xinit,isNegative,dx,dy);

    }

    /**
     * A class representing the ActiveEdgeList for the fillPolygon algorithm
     *
     * Author Stephen Yingling
     */
    class ActiveEdgeList{
        protected ArrayList<EdgeBucket> list;   //The list of EdgeBuckets

        /**
         * Creates a new ActiveEdgeList
         */
        public ActiveEdgeList(){
            list = new ArrayList<EdgeBucket>();
        }

        /**
         * Adds the EdgeBucket e and all the EdgeBuckets that follow it
         * to the ActiveEdgeList
         * @param e The first EdgeBucket in a linked list of EdgeBuckets to add
         */
        public void add(EdgeBucket e){
            EdgeBucket cur = e;

            while (cur != null){
                list.add(cur);
                cur = cur.getNext();
            }
        }

        /**
         * Removes all EdgeBuckets with ymax <= y
         * @param y The current scanline
         */
        public void removePassedEdges(int y){
            for(int i = 0; i< list.size(); i++){
                if(list.get(i).getYmax() <= y){
                    list.remove(i);
                    i--;
                }
            }
        }

        /**
         * Sorts the EdgeBuckets in the ActiveEdgeList
         */
        public void sort(){
            Collections.sort(list, new EdgeBucketComparator());
        }

        /**
         * Gets the EdgeBucket at the given index
         * @param index The index of the bucket to be retrieved
         * @return The EdgeBucket at the given index
         */
        public EdgeBucket get(int index){
            return list.get(index);
        }

        /**
         * Gets the size of the ActiveEdgeList
         * @return The size of the ActiveEdgeList
         */
        public int size(){
            return list.size();
        }

    }

    /**
     * A class representing the EdgeTable
     *
     * Author: Stephen Yingling
     */
    class EdgeTable{
        protected EdgeBucket[] table;   //The table itself
        protected int ymin;             //Used for indexing

        /**
         * Create an edgetable for edges between (inclusive) ymin and ymax
         * @param ymax The maximum y value of the EdgeBuckets in the table
         * @param ymin The minimum y value of the EdgeBuckets in the table
         */
        public EdgeTable(int ymax, int ymin){
            table = new EdgeBucket[1+ymax-ymin];
            this.ymin = ymin;
        }

        /**
         * Adds an EdgeBucket to the EdgeTable
         * @param e The EdgeBucket to be added
         */
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

        /**
         * Gets the first EdgeBucket entry at the given index
         * @param y The index of the EdgeTable (normally the current y value)
         * @return The first EdgeBucket at the given index
         */
        public EdgeBucket getFirstEB(int y){
            int tIndex = y - ymin;
            return table[tIndex];
        }
    }


    /**
     * Represents an EdgeBucket
     * Author: Stephen Yingling
     */
    class EdgeBucket {
        protected int firtscan; //The first scan line this edge is encountered
        protected int ymax; //The scanline at which this edge should be removed
        protected int xVal;    //The x value for this EdgeBucket
        protected boolean isNegative;   //Whether or not the slope is negative
        protected int dx;   //The dx component of this edge
        protected int dy;   //The dy component of this edge
        protected int sum;  //The sum component of this edge
        protected EdgeBucket next;  //The next EdgeBucket in the list

        /**
         * Create an EdgeBucket with the given values
         * @param firstscan The first scan line this edge is encountered
         * @param ymax The scanline at which this edge should be removed
         * @param xinit The initial x value for this edge
         * @param isNegative Whether or not the slope is negative
         * @param dx The dx component of this edge
         * @param dy The dy component of this edge
         */
        public EdgeBucket(int firstscan, int ymax, int xinit, boolean isNegative, int dx, int dy){
            this.firtscan = firstscan;
            this.ymax = ymax;
            this.xVal = xinit;
            this.isNegative = isNegative;
            this.dx =dx;
            this.dy = dy;
            this.sum = 0;   //Always starts at 0
        }

        /**
         * Updates the sum and Xval variables by:
         * 1. adding dx to sum
         * 2. incrementing/decrementing xVal when sum > dy
         */
        public void updateSumAndX(){
            sum += dx;
            if(dx != 0 && sum >= dy){
                while (sum > dy){
                    sum -=dy;
                    if(isNegative){
                        xVal--;
                    }else{
                        xVal++;
                    }
                }
            }
        }

        /**
         * Gets the sum component
         * @return The value of sum
         */
        public int getSum() {
            return sum;
        }

        /**
         * Gets the next EdgeBucket in the list
         * @return The next EdgeBucket in the list
         */
        public EdgeBucket getNext() {
            return next;
        }

        /**
         * Sets the next EdgeBucket in the list
         * @param next The next EdgeBucket in the list
         */
        public void setNext(EdgeBucket next) {
            this.next = next;
        }

        /**
         * Gets the first scanline this edge crosses
         * @return The first scanline this edge crosses
         */
        public int getFirtscan() {
            return firtscan;
        }

        /**
         * Gets the maximum y value for this EdgeBucket
         * @return The maximum y value for this EdgeBucket
         */
        public int getYmax() {
            return ymax;
        }

        /**
         * Returns the current x value for this EdgeBucket
         * @return The current x value for this EdgeBucket
         */
        public int getxVal() {
            return xVal;
        }

        /**
         * Gets whether the slope is negative or not
         * @return True if the slope is negative, false if otherwise
         */
        public boolean isNegative() {
            return isNegative;
        }

        /**
         * Gets the dx component of this edge
         * @return The dx component of this edge
         */
        public int getDx() {
            return dx;
        }

        /**
         * Gets the dy component of this edge
         * @return The dy component of this edge
         */
        public int getDy() {
            return dy;
        }

    }

    /**
     * A comparator for sorting EdgeBuckets
     * Author: Stephen Yingling
     */
    class EdgeBucketComparator implements Comparator<EdgeBucket>{

        /**
         * Compares two edgeBuckets by their xVal.
         * If both xVals are equal, compares on 1/m
         * @param o1 The first EdgeBucket
         * @param o2 The second EdgeBucket
         * @return The integer comparison value of the o1 vs o2
         */
        @Override
        public int compare(EdgeBucket o1, EdgeBucket o2) {

            //Compare xVals
            if (o1.getxVal() != o2.getxVal()){
                Integer x1, x2;
                x1 = o1.getxVal();
                x2 = o2.getxVal();
                return x1.compareTo(x2);
            }

            //Compare based on 1/m
            Float mInv1 = ((float)o1.getDx()/(float)o1.getDy());
            Float mInv2 = ((float)o2.getDx()/(float)o2.getDy());
            return mInv1.compareTo(mInv2);
        }
    }
    
}
