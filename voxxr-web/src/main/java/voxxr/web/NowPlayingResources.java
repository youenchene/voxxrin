package voxxr.web;

import com.google.appengine.api.datastore.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * User: xavierhanin
 * Date: 1/28/12
 * Time: 8:51 PM
 */
public class NowPlayingResources implements RestRouter.RequestHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, Map<String, String> params) throws IOException {
        String eventId = params.get("eventId");
        String kind = "PresentationHeader";
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            Rests.sendAsJsonArray(
                datastore.prepare(new Query(kind)
                        .addFilter("nowplaying", Query.FilterOperator.EQUAL, true)
                        .addFilter("eventId", Query.FilterOperator.EQUAL, eventId))
                        .asIterable(FetchOptions.Builder.withLimit(100)), resp);
        } else if ("POST".equalsIgnoreCase(req.getMethod())) {
            if (!Rests.isSecure(req)) {
                resp.sendError(403, "Unauthorized");
                return;
            }
            try {
                JSONObject command = Rests.jsonObjectFromRequest(req);
                String id = command.getString("id");
                String action = command.getString("action");
                try {
                    Entity entity = datastore.get(Rests.createKey(kind, id));
                    if ("start".equals(action)) {
                        entity.setProperty("nowplaying", true);
                    } else if ("stop".equals(action)) {
                        entity.setProperty("nowplaying", false);
                    } else {
                        resp.sendError(400, "Unknwon action " + action);
                    }
                    datastore.put(entity);
                } catch (EntityNotFoundException e) {
                    resp.sendError(400, "Unknown presentation to " + action);
                }
            } catch (JSONException e) {
                resp.sendError(400, "Invalid json: " + e.getMessage());
            }
        }
    }
}

