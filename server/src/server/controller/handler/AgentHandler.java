package server.controller.handler;

import server.Simulator;
import server.model.Agent;
import server.model.Coordinate;
import tool.HttpServer.Request;
import tool.HttpServer.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AgentHandler extends RestHandler {

    public AgentHandler(String handlerName, Simulator simulator) {
        super(handlerName, simulator);
    }

    @Override
    public void handlePost(Request req, Response resp) throws IOException {
        String rPath = parseRemainingPath(req.getPath());
        // /agents
        if(rPath == null)
            handleAdd(req, resp);
        // /agents/time-out/<id>
        else if(rPath.startsWith("/time-out"))
            handleTimeout(req, resp, rPath.replace("/time-out/", ""));
        // /agents/route/add/<id>
        else if(rPath.startsWith("/route/add"))
            handleRouteAdd(req, resp, rPath.replace("/route/add/", ""));
        // /agents/route/add/<id>
        else if(rPath.startsWith("/route/edit"))
            handleRouteEdit(req, resp, rPath.replace("/route/edit/", ""));
        // /agents/<id>
        else
            handleUpdate(req, resp, rPath.substring(1));
    }

    @Override
    public void handleDelete(Request req, Response resp) throws IOException {
        String rPath = parseRemainingPath(req.getPath());
        // /agents/route/<id>
        if(rPath.startsWith("/route"))
            handleDeleteWaypoint(req, resp, rPath.replace("/route/", ""));
        // /agents/<id>
        else
            handleAgentDelete(resp, rPath.substring(1));
    }

    private void handleAdd(Request req, Response resp) throws IOException {
        Map<String, String> params = req.getParams();
        List<String> expectedKeys = Arrays.asList("lat", "lng", "heading");
        if (!checkParams(params, expectedKeys, resp))
            return;
        double lat = Double.parseDouble(params.get("lat"));
        double lng = Double.parseDouble(params.get("lng"));
        double heading = Double.parseDouble(params.get("heading"));
        Agent agent = simulator.getAgentController().addVirtualAgent(lat, lng, heading);
        resp.send(201, "Created new agent " + agent.getId());
    }

    private void handleUpdate(Request req, Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        Map<String, String> params = req.getParams();
        if (params.containsKey("speed"))
            simulator.getAgentController().updateAgentSpeed(id, Double.parseDouble(params.get("speed")));
        if (params.containsKey("altitude"))
            simulator.getAgentController().updateAgentAltitude(id, Double.parseDouble(params.get("altitude")));
        resp.sendOkay();
    }

    private void handleTimeout(Request req, Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        Map<String, String> params = req.getParams();
        List<String> expectedKeys = Collections.singletonList("timedOut");
        if (!checkParams(params, expectedKeys, resp))
            return;
        boolean timedOut = Boolean.parseBoolean(params.get("timedOut"));
        if(simulator.getAgentController().setAgentTimedOut(id, timedOut))
            resp.sendOkay();
        else
            resp.sendError(400, "Unable to updated agent timedOut " + id);
    }

    private void handleRouteAdd(Request req, Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        Map<String, String> params = req.getParams();
        List<String> expectedKeys = Arrays.asList("index", "lat", "lng");
        if (!checkParams(params, expectedKeys, resp))
            return;
        int index = Integer.parseInt(params.get("index"));
        double lat = Double.parseDouble(params.get("lat"));
        double lng = Double.parseDouble(params.get("lng"));
        simulator.getAgentController().addToAgentTempRoute(id, index, new Coordinate(lat, lng));
        resp.sendOkay();
    }

    private void handleRouteEdit(Request req, Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        Map<String, String> params = req.getParams();
        List<String> expectedKeys = Arrays.asList("index", "lat", "lng");
        if (!checkParams(params, expectedKeys, resp))
            return;
        int index = Integer.parseInt(params.get("index"));
        double lat = Double.parseDouble(params.get("lat"));
        double lng = Double.parseDouble(params.get("lng"));
        simulator.getAgentController().editAgentTempRoute(id, index, new Coordinate(lat, lng));
        resp.sendOkay();
    }

    private void handleDeleteWaypoint(Request req, Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        Map<String, String> params = req.getParams();
        List<String> expectedKeys = Collections.singletonList("index");
        if (!checkParams(params, expectedKeys, resp))
            return;
        int index = Integer.parseInt(params.get("index"));
        simulator.getAgentController().deleteFromAgentTempRoute(id, index);
        resp.sendOkay();
    }

    private void handleAgentDelete(Response resp, String id) throws IOException {
        if (!agentExists(id, resp))
            return;
        if (simulator.getAgentController().deleteAgent(id))
            resp.sendOkay();
        else
            resp.sendError(400, "Unable to delete agent " + id);
    }

}
