package org.ggp.base.player.gamer.raho;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static java.util.Arrays.asList;

/**
 * User: rafal.hotlos
 * Date: 24/11/13
 * Time: 09:55
 */
public class MCTSPlayer extends SampleGamer {

    private long finishBy;


    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {

        long start = System.currentTimeMillis();

        finishBy = timeout - 1000;

        Node root = new Node(getCurrentState(), null, null);

        while (System.currentTimeMillis() < finishBy) {
//        while (root.visits < 100) {
            // selection
            Node selectedNode = select(root);
            System.out.printf("selected: %s\n", selectedNode);
            // expansion
            Node expandedNode = expand(selectedNode);
            System.out.printf("expanded: %s\n", expandedNode);
            // simulation
            int score = simulateFrom(expandedNode);
            System.out.printf("score: %s\n", score);
            // backpropagation
            expandedNode.backpropagate(score);
        }


        Move selection = bestMove(root);

        long stop = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
                getRole());
        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop
                - start));
        return selection;
    }

    private Node select(Node node) {
        if (node.visits == 0) {
            return node;
        }
        for (Node child : node.children) {
            if (child.visits == 0) {
                return child;
            }
        }
        if (node.children.isEmpty()) {
            return node;
        }

        int score = 0;
        Node result = node;

        for (Node child : node.children) {
            int newScore = child.selectfn();
            if (newScore > score) {
                score = newScore;
                result = child;
            }
        }
        return select(result);
    }

    private Node expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();

        if (stateMachine.isTerminal(node.state)) {
            return node;
        }
        List<Move> legalMoves = stateMachine.getLegalMoves(node.state, getRole());
//        List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(state, getRole(), action);

        for (Move legalMove : legalMoves) {
            MachineState newState = stateMachine.getNextState(node.state, asList(legalMove));
            Node newNode = new Node(newState, legalMove, node);
            node.children.add(newNode);
        }

        return node.children.get(0);
    }

    private int simulateFrom(Node node) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();

        if (stateMachine.isTerminal(node.state)) {
            int goal = stateMachine.getGoal(node.state, getRole());
            System.out.printf("instead of simulation returning goal=%s (state is terminal)", goal);
            return goal;
        }

        long simulateStart = System.currentTimeMillis();
        int[] depthChargeResult = new int[1];
        MachineState finalState = stateMachine.performDepthCharge(node.state, depthChargeResult);
        int goal = stateMachine.getGoal(finalState, getRole());
        long simulateTime = System.currentTimeMillis() - simulateStart;
        System.out.printf("simulation done in %s millis\n", simulateTime);
        return goal;
    }

    private Move bestMove(Node node) {
        System.out.println("best move start");
        int maxVisits = 0;
        Move result = null;
        for (Node child : node.children) {
            System.out.printf("  best move child visits/utility = %s/%s", child.visits, child.utility);
            if (child.visits > maxVisits) {
                maxVisits = child.visits;
                result = child.move;
            }
        }
        System.out.println("best move finished");
        return result;
    }


    private static class Node {
        public MachineState state;
        public Move move;
        public int visits = 0;
        public int utility = 0;
        public Node parent;
        public List<Node> children = new ArrayList<Node>();

        public int depth = 0;
        public String description;

        private Node(MachineState state, Move move, Node parent) {
            this.state = state;
            this.move = move;
            this.parent = parent;

            int pos = 0;
            if (parent != null) {
                pos = parent.children.size();
                depth = parent.depth + 1;
            }
            this.description = String.format("depth:%s,pos:%s", depth, pos);
        }

        public int selectfn() {
            int result = utility;
            if (parent != null) {
                result += sqrt(log(parent.visits) / visits);
            }
            return result;
        }

        public void backpropagate(int score) {
            visits++;
            utility += score;
            if (parent != null) {
                parent.backpropagate(score);
            }
        }

        @Override
        public String toString() {
            return "Node[" + description + ']';
        }
    }

}
