package modelui.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.json.JsonObject;
import javax.xml.transform.TransformerException;

import modelui.server.job.request.handler.RequestHandler;
import modelui.util.json.JSONHelper;
import modelui.util.mesh.Mesh;
import modelui.util.mesh.RelationalTreeNode;
import modelui.util.mesh.creator.RelationalJSONNodeCreator;
import modelui.util.mesh.graphviz.GraphvizTreeVisitor;
import modelui.util.mesh.mermaid.MermaidSubsystemTreeVisitor;
import modelui.util.mesh.springy.SpringyTreeVisitor;
import modelui.util.tree.xpath.TreeNodeXPathExecuterImpl;
import org.apache.commons.lang3.StringUtils;
import org.rapidoid.buffer.Buf;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
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
        On.post("/tograph").plain((Req req) -> {
            Map<String, Object> payload = req.data();
            String systemDefinition = String.valueOf(payload.get("systemDefJSON"));
            String filter = String.valueOf(payload.get("filterXPath"));
            String outputType = String.valueOf(payload.get("outputType"));
            int filterLevel = Integer.valueOf(Objects.toString(payload.get("filterLevel"), "1")).intValue();
            filter = (StringUtils.isEmpty(filter) ? "/*" : filter);
            return toGraph(systemDefinition, filter, filterLevel, outputType);


        });
        On.post("/springy").plain((Req req) -> {
            Map<String, Object> payload = req.data();
            String systemDefinition = String.valueOf(payload.get("systemDefJSON"));
            String filter = String.valueOf(payload.get("filterXPath"));
            int filterLevel = Integer.valueOf(Objects.toString(payload.get("filterLevel"), "1")).intValue();
            return toSpringy(systemDefinition, StringUtils.isEmpty(filter) ? "/*" : filter, filterLevel);
        });
    }

    private static String toGraph(String systemDefinition, String filter, int filterLevel, String outputType) {
        RelationalTreeNode node = new RelationalTreeNode();
        node.setMesh(new Mesh());
        node.getMesh().addRoot(node);
        JsonObject aDef = JSONHelper.createJson(systemDefinition);
        node.parse(aDef.entrySet().iterator().next().getValue(), new RelationalJSONNodeCreator(node.getMesh()));
        node.initialize();
        if ("mermaid".equals(outputType)) {
            return MermaidSubsystemTreeVisitor.toGraph(filterLevel, node.getMesh().createSystemTree(filterLevel, null)).toString();
        } else if ("springy".equals(outputType)) {
            return SpringyTreeVisitor.toGraph(filterLevel, node.getMesh().createSystemTree(filterLevel, null)).toString();

        } else if ("graphviz".equals(outputType)) {
            return GraphvizTreeVisitor.toGraph(filterLevel, node.getMesh().createSystemTree(filterLevel, null), null).toString();

        }
        return null;
    }


    public static String toSpringy(String json, String filterXPath, int filterLevel) throws IOException, TransformerException {
        RelationalTreeNode node = new RelationalTreeNode();
        node.setMesh(new Mesh());
        node.getMesh().addRoot(node);
        JsonObject aDef = JSONHelper.createJson(json);
        node.parse(aDef.entrySet().iterator().next().getValue(), new RelationalJSONNodeCreator(node.getMesh()));
        node.initialize();
        List<RelationalTreeNode> selection = (List<RelationalTreeNode>) TreeNodeXPathExecuterImpl.getInstance().processXPathJaxen(filterXPath, node);
        return SpringyTreeVisitor.toGraph(filterLevel, node.getMesh().createSystemTree(filterLevel, new HashSet<>(selection))).toString();
    }
}
