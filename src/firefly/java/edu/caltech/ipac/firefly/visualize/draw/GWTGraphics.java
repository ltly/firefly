/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.ScreenPt;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Oct 1, 2008
 * Time: 11:21:49 AM
 */



/**
 * @author Trey Roby
 */
public class GWTGraphics implements Graphics {

    private final GWTCanvas surfaceW;
    private final GWTCanvasPanel _canvasPanel;
    private final List<CanvasLabelShape> _labelList= new ArrayList<CanvasLabelShape>(20);

 //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public GWTGraphics() {

        _canvasPanel= new GWTCanvasPanel();
        surfaceW = _canvasPanel.getCanvas();
    }

    public Widget getWidget() { return _canvasPanel; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public void drawLine(String color,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {

        drawLine(color, DEF_WIDTH,sx,sy,ex,ey);
    }

    public void drawLine(String color,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.beginPath();
        surfaceW.moveTo(sx,sy);
        surfaceW.lineTo(ex,ey);
        surfaceW.closePath();
        surfaceW.stroke();
    }

    public void drawPath(String color,
                         int lineWidth,
                         List<ScreenPt> pts,
                         boolean close) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.beginPath();

        boolean first= true;
        for(ScreenPt pt : pts) {
            if (first) {
                first=  false;
                surfaceW.moveTo(pt.getX(),pt.getY());
            }
            else {
                surfaceW.lineTo(pt.getX(),pt.getY());
            }
        }
        if (close) surfaceW.closePath();
        surfaceW.stroke();
    }

    public void drawPath(String color,
                         int lineWidth,
                         List<PathType> ptList) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.beginPath();

        boolean first= true;
        for(PathType pT : ptList) {
            if (!pT.isDraw() || first) {
                surfaceW.moveTo(pT.getX(),pT.getY());
                first=  false;
            }
            else {
                surfaceW.lineTo(pT.getX(),pT.getY());
            }
        }
        surfaceW.stroke();
    }




    public void beginPath(String color, int lineWidth) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.beginPath();
    }

    public void pathMoveTo(int x,int y) {
        surfaceW.moveTo(x,y);

    }

    public void pathLineTo(int x,int y) {
        surfaceW.lineTo(x,y);
    }

    public void rect(int x, int y, int width, int height) {
        surfaceW.rect(x,y,width,height);
    }

    public void arc(int x,int y, double radius, double startAngle, double endAngle) {
        surfaceW.arc(x,y,radius,startAngle,endAngle,false);
    }

    public void drawPath() {
        surfaceW.stroke();
    }




    public void drawText(String color, String size, int x, int y, String text) {
        drawText(color, "inherit", size, "normal",  "normal", x, y, text);
    }

    public void drawText(String color,
                         String fontFamily,
                         String size,
                         String fontWeight,
                         String fontStyle,
                         int x,
                         int y,
                         String text) {


        HTML label= DrawUtil.makeDrawLabel(color, fontFamily, size, fontWeight, fontStyle, text);
        CanvasLabelShape labelShape= new CanvasLabelShape(label);
        _labelList.add(labelShape);
        _canvasPanel.addLabel(label,x,y);
    }

    public void drawCircle(String color, int lineWidth, int x, int y, int radius) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.arc(x,y,radius,0,2* Math.PI,false);
        surfaceW.stroke();

    }

    public void drawRec(String color,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height) {
        surfaceW.setLineWidth(lineWidth);
        surfaceW.setStrokeStyle(makeColor(color));
        surfaceW.beginPath();
        surfaceW.moveTo(x,y);
        surfaceW.lineTo(x+width,y);
        surfaceW.lineTo(x+width,y+height);
        surfaceW.lineTo(x,y+height);
        surfaceW.lineTo(x,y);
        surfaceW.closePath();
        surfaceW.stroke();
    }


    public void fillRec(String color,
                        int x,
                        int y,
                        int width,
                        int height) {

        surfaceW.setLineWidth(1);
//        surfaceW.setStrokeStyle(getColor());
        surfaceW.setFillStyle(makeColor(color));
        surfaceW.fillRect(x,y,width,height);
        surfaceW.stroke();

    }


    public void clear() {
        surfaceW.clear();
        for(CanvasLabelShape label : _labelList) {
            _canvasPanel.removeLabel(label.getLabel());
        }
        _labelList.clear();
    }

    public void paint() { }

    public void setDrawingAreaSize(int width, int height) {
        if(surfaceW.getOffsetWidth()!=width ||
           surfaceW.getOffsetHeight()!=height ||
           surfaceW.getCoordWidth()!=width ||
           surfaceW.getCoordHeight()!=height) {

            surfaceW.setPixelSize(width,height);
            surfaceW.setCoordSize(width, height);
            _canvasPanel.setPixelSize(width,height);
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static Color makeColor(String c)  {
        if (GwtUtil.isHexColor(c)) c= "#" + c;
        return new Color(c);
    }

// =====================================================================
// -------------------- Native Methods --------------------------------
// =====================================================================


}
