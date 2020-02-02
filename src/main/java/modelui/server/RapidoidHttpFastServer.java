package modelui.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.json.JsonObject;
import javax.xml.transform.TransformerException;

import modelui.server.job.request.handler.RequestHandler;
import modelui.util.json.JSONHelper;
import modelui.util.mesh.Mesh;
import modelui.util.mesh.RelationalTreeNode;
import modelui.util.mesh.creator.RelationalJSONNodeCreator;
import modelui.util.mesh.graphviz.GraphvizTreeVisitor;
import modelui.util.tree.xpath.TreeNodeXPathExecuterImpl;
import org.rapidoid.buffer.Buf;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.http.Req;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import org.rapidoid.setup.On;


public class RapidoidHttpFastServer extends AbstractHttpServer {
    private static final int port = 7090;
    private static final byte ALERTER_PATH[] = "/alerter".getBytes();
    private static final String HELLO_WORLD = "Hello, World!";
    private List<RequestHandler> handlerList = new ArrayList<RequestHandler>();

    @Override
    protected HttpStatus handle(Channel ctx, Buf buf, RapidoidHelper req) {
        for (RequestHandler handler : handlerList) {
            if (handler.matches(ctx, buf, req.path)) {
                return handler.handle(ctx, buf, req.path);
                // serializeToJson(HttpUtils.noReq(), ctx, req.isKeepAlive.value, HELLO_WORLD);
            }
        }
        //so
        return HttpStatus.NOT_FOUND;
    }

    public static void main(String[] args) throws Exception {
        new RapidoidHttpFastServer().listen(port);
        On.get("/size").json((String msg) -> msg.length());
        On.post("/graph").plain((Req req) -> {
          return toGraphviz(new String(req.body()), req.param("filter"));
            //handle aid and type
        });
    }

    //
    public String toMermaidGraph2(String definition, String filterXPath) throws IOException {
//        String json = FileUtils.readAsStringFromClass(this.getClass(), "systems.json", "utf-8");
        RelationalTreeNode node = new RelationalTreeNode();
//        node.setMesh(new Mesh());
//        node.getMesh().addRoot(node);
//        JsonObject aDef = JSONHelper.createJson(json);
//        node.parse(aDef.entrySet().iterator().next().getValue(), new RelationalJSONNodeCreator(node.getMesh()));
//        node.initialize();
//
//        System.out.println(GraphvizTreeVisitor.toGraph(9, node.getMesh().createSystemTree(9, null)));
        return "";
    }

    public static String toGraphviz(String json, String filterXPath) throws IOException, TransformerException {
        // String json = FileUtils.readAsStringFromClass(RapidoidHttpFastServer.class, "systems.json", "utf-8");
        RelationalTreeNode node = new RelationalTreeNode();
        node.setMesh(new Mesh());
        node.getMesh().addRoot(node);
        JsonObject aDef = JSONHelper.createJson(json);
        node.parse(aDef.entrySet().iterator().next().getValue(), new RelationalJSONNodeCreator(node.getMesh()));
        node.initialize();
        node.hasRelations();
        node.findRelations();

        List<RelationalTreeNode> selection = (List<RelationalTreeNode>) TreeNodeXPathExecuterImpl.getInstance().processXPathJaxen(filterXPath, node);
        return GraphvizTreeVisitor.toGraph(9, node.getMesh().createSystemTree(9, new HashSet<>(selection)), null).toString();
    }
}
