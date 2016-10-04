package game;

import java.io.Serializable;

// Point2D implementation

public class Point2D implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 5316500891829875595L;
	
	double x;
    double y;
    
    public Point2D(double x, double y){
        this.x = x;
        this.y = y;
    }
    
    public Point2D(){
        x = 0;
        y = 0;
    }
    
    public double distance(Point2D oth){
        return Math.sqrt((x-oth.x)*(x-oth.x)+(y-oth.y)*(y-oth.y));
    }
    
    public double distanceSq(Point2D oth){
        return (x-oth.x)*(x-oth.x)+(y-oth.y)*(y-oth.y);
    }
    
    @Override
    public Point2D clone(){
        return new Point2D(x,y);
    }
}

