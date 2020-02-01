package modelui.server.job.request.handler;

import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.net.abstracts.Channel;

public interface RequestHandler {
    boolean matches(Channel ctx, Buf buf, BufRange path);

    HttpStatus handle(Channel ctx, Buf buf, BufRange path);
}
