package edu.caltech.ipac.hydra.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.hydra.data.PtfRequest;
import edu.caltech.ipac.hydra.server.query.PtfProcimsFileRetrieve;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SearchProcessorImpl(id = "PtfDownload")
public class PtfFileGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private final int L1_FITS_SIZE = 33586560;
    private final int L1_MASK_SIZE = 16781760;
    
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        // create unique list of filesystem-based and url-based files
        Set<String> zipFiles = new HashSet<String>();

        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;
        
        String procImage = request.getParam("procImage");
        boolean dlpImage = false;
        if (procImage.equalsIgnoreCase("yes")) {
            dlpImage = true;
        }

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders =  zipType != null && zipType.equalsIgnoreCase("folder");

        // build file types list
        String artFiles = request.getParam("anciFiles");

        List<String> types = new ArrayList<String>();
        if (dlpImage){
            types.add(PtfRequest.PIMAGE);
        }
        if (artFiles != null && artFiles.length() > 0 && !artFiles.equalsIgnoreCase("_none_")) {
            String[] artArr = artFiles.split(",");
            for (String art : artArr) {
                if (art.equalsIgnoreCase("M")) {
                    types.add(PtfRequest.MIMAGE);
                } else if (art.equalsIgnoreCase("R")) {
                    types.add(PtfRequest.RIMAGE);
                } else if (art.equalsIgnoreCase("S")) {
                    types.add(PtfRequest.SECATL);
                } else if (art.equalsIgnoreCase("P")) {
                    types.add(PtfRequest.PSFCATL);
                }
            }
        }

        String baseFilename = PtfProcimsFileRetrieve.PTF_FILESYSTEM_BASEPATH;

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, PtfRequest.PIMAGE, PtfRequest.MIMAGE, PtfRequest.RIMAGE, PtfRequest.SECATL, PtfRequest.PSFCATL, "in_ra", "crval1", "in_dec", "crval2");

        String baseUrl = PtfProcimsFileRetrieve.getBaseURL(request);
        String subSize = request.getSafeParam("subsize");
        double sizeD = StringUtils.isEmpty(subSize) ? 0 : Double.parseDouble(subSize);
        String sizeAsecStr = String.valueOf((int)(sizeD * 3600));

        for (int rowIdx : selectedRows) {

            for(String col : types) {
                FileInfo fi = null;
                String fName = (String) dgData.get(rowIdx, col);

                if (StringUtils.isEmpty(fName)) {
                    logger.warn("No file name.  column=" + col);
                    continue;
                }

                File f = new File(baseFilename, fName);

                String extName = doFolders ? fName : f.getName();
                if (doCutout && ( col.equals(PtfRequest.PIMAGE) || col.equals(PtfRequest.MIMAGE) ) ) {
                    //long estSize = 5000;
                    int estSize;
                    // PTF pixscal = 1.01 arcsec/pix

                    double ratiol = sizeD / ((2048 * 1.01) / 3600);  // pixel length * asec/pixel / 3600
                    double ratioh = 0.5 * ratiol; // ratioh = sizeD / ((4096 * 1.01) / 3600);pixel height * asec/pixel / 3600
                    if (ratiol < 1.0) {
                         estSize = (int) (((L1_FITS_SIZE - 31750) * (ratiol*ratioh)) + 31750);  // 31750 = L1 header size
                    } else {
                         estSize = L1_FITS_SIZE;
                    }
                    if (col.equals(PtfRequest.MIMAGE)){
                    		estSize = (int)(0.5*estSize);
                    }
                    
                    // look for in_ra and in_dec returned by IBE
                    String subLon = String.format("%.4f", (Double)dgData.get(rowIdx, "in_ra"));
                    if (StringUtils.isEmpty(subLon)) {
                      subLon = String.format("%.4f", (Double)dgData.get(rowIdx, "crval1"));
                    }

                    // look for in_ra and in_dec returned by IBE
                    String subLat = String.format("%.4f", (Double)dgData.get(rowIdx, "in_dec"));
                    if (StringUtils.isEmpty(subLat)) {
                      // if it fails, try using crval2
                        subLat = String.format("%.4f", (Double)dgData.get(rowIdx, "crval2"));
                    }
                    String cutoutInfo = "_ra" + subLon + "_dec" + subLat + "_asec" + sizeAsecStr;

                    String url = PtfProcimsFileRetrieve.createCutoutURLString_l1(baseUrl, fName, subLon, subLat, subSize);
                    // strip out filename when using file resolver
                    if (doFolders) {
                        int idx = extName.lastIndexOf("/");
                        idx = idx < 0 ? 0 : idx;
                        extName = extName.substring(0, idx) + "/";
                    } else {
                        extName = null;
                    }

                    fi = new FileInfo(url, new CutoutFilenameResolver(cutoutInfo, extName), estSize);
                } else {
                    fi = new FileInfo(f.getAbsolutePath(), extName, f.length());
                }
                if (fi != null) {
                    fiArr.add(fi);
                    fgSize += fi.getSizeInBytes();
                }
            }
        }
        FileGroup fg = new FileGroup(fiArr, null, fgSize, "PTF Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
        return fgArr;
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
