package com.nyukob.ghosttransfer;
import fi.iki.elonen.NanoHTTPD;
import java.io.FileInputStream;
public class GhostHttpServer extends NanoHTTPD {
    public static final int PORT = 12500;
    public GhostHttpServer() throws java.io.IOException { super(PORT); }
    @Override public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String ip = session.getRemoteIpAddress();
        try {
            switch(uri) {
                case "/ping": return newFixedLengthResponse("OK");
                case "/info": {
                    java.io.File f = ShareState.filePath;
                    String hf = (f!=null&&f.exists())?"1":"0", fn = "1".equals(hf)?f.getName():"", ht = ShareState.text.isEmpty()?"0":"1";
                    ShareState.addLog(ip+" - /info");
                    return newFixedLengthResponse(Response.Status.OK,"text/plain; charset=utf-8","HasFile="+hf+"\nFileName="+fn+"\nHasText="+ht);
                }
                case "/text": ShareState.addLog(ip+" - /text"); return newFixedLengthResponse(Response.Status.OK,"text/plain; charset=utf-8",ShareState.text);
                case "/file": {
                    java.io.File f = ShareState.filePath;
                    if(f!=null&&f.exists()){
                        ShareState.addLog(ip+" - /file: "+f.getName());
                        Response r = newFixedLengthResponse(Response.Status.OK,"application/octet-stream",new FileInputStream(f),f.length());
                        r.addHeader("Content-Disposition","attachment; filename=\""+f.getName()+"\"");
                        return r;
                    }
                    return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","No file");
                }
                default: return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","404");
            }
        } catch(Exception e){ return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/plain","Error: "+e.getMessage()); }
    }
}
