package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Aug 31, 2010
 * Time: 10:49:36 AM
 */


/**
 * @author Trey Roby
 * This class is only used on the client
 */
public class CompositeJob {

    private final BackgroundState state;
    private final List<BackgroundStatus> partList;
    private String id;

    public CompositeJob(String id, List<BackgroundStatus> partList) {
        this.state= computeState(partList);
        this.partList= partList;
        this.id= id;
    }

    /**
     * Creates a new report based on this but with the one BackgroundReport changed.
     * @param deltaPart the Background report that changed
     * @return a new Composite report
     */
    public CompositeJob makeDeltaJob(BackgroundStatus deltaPart) {
        List<BackgroundStatus> list= new ArrayList<BackgroundStatus>(partList);
        for(BackgroundStatus part : partList) {
            if (ComparisonUtil.equals(part.getID(), deltaPart.getID())) {
                list.add(deltaPart);
            }
            else {
                list.add(part);
            }
        }
        return new CompositeJob(id, list);
    }


    public List<BackgroundStatus> getPartList() { return partList; }


    private static BackgroundState computeState(List<BackgroundStatus> reportParts) {
        BackgroundState state= null;
        for(BackgroundStatus s : reportParts) {
            switch (s.getState()) {

                case WAITING:
                    state= s.getState();
                    break;
                case STARTING:
                    if (state==null) state= s.getState();
                    break;
                case WORKING:
                    state= BackgroundState.WAITING;
                    break;
                case SUCCESS:
                    if (state==null) state= s.getState();
                    break;
                case USER_ABORTED:
                case UNKNOWN_PACKAGE_ID:
                case FAIL:
                case CANCELED:
                    state= s.getState();
                    break;
            }


            if (state==BackgroundState.FAIL ||
                state==BackgroundState.CANCELED ||
                state==BackgroundState.UNKNOWN_PACKAGE_ID ||
                state==BackgroundState.USER_ABORTED) {
                break;
            }
        }
        return state;

    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */