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

    private static enum TYPE {
        MAX, MIN;
    }


    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {

        long start = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
                getRole());

        Move selection = null;

        if (moves.size() == 1) {
            selection = moves.get(0);
        } else {


            finishBy = timeout - 1000;
//        finishBy = start + 2000;

            Node root = new Node(getCurrentState(), null, null, TYPE.MAX);

            while (System.currentTimeMillis() < finishBy) {
//        while (root.visits < 100) {
                // selection
                Node selectedNode = select(root);
                if (selectedNode.toString().length() < 20) {
                    System.out.printf("selected: %s\n", selectedNode);
                } else  {
//                System.out.print('.');
                }
                // expansion
                expand(selectedNode);
//            System.out.printf("expanded: %s\n", expandedNode);
                // simulation

                int score = simulateFrom(selectedNode);
//            System.out.printf("score: %s\n", score);
                // backpropagation
                selectedNode.backpropagate(score);
            }

            selection = bestMove(root);
        }

        long stop = System.currentTimeMillis();

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

        double score = node.type == TYPE.MAX ? 0 : 100000;
        Node result = node;

        for (Node child : node.children) {
            double newScore = child.selectfn();
//            System.out.println("score: " + newScore);
            if (node.type == TYPE.MAX) {
                if (newScore >= score) {
                    score = newScore;
                    result = child;
                }
            } else {
                if (newScore < score) {
                    score = newScore;
                    result = child;
                }
            }
        }
//        System.out.println("subselecting from: " + result);
        return select(result);
    }

    private void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();

        if (stateMachine.isTerminal(node.state)) {
            return;
        }

        if (node.type == TYPE.MAX) {
            List<Move> myMoves = stateMachine.getLegalMoves(node.state, getRole());
//            System.out.println("adding nodes " + myMoves.size());
            for (Move myMove : myMoves) {
                Node newMinNode = new Node(node.state, asList(myMove), node, TYPE.MIN);
                node.children.add(newMinNode);
            }
        } else {
            List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(node.state, getRole(), node.moves.get(0));
//            System.out.println("adding nodes " + jointMoves.size());
            for (List<Move> jointMove : jointMoves) {
                MachineState newState = stateMachine.getNextState(node.state, jointMove);
                Node newMaxNode = new Node(newState, jointMove, node, TYPE.MAX);
                node.children.add(newMaxNode);
            }
        }
//        return node.children.get(0).children.get(0);
    }

    private int simulateFrom(Node node) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();

        if (stateMachine.isTerminal(node.state)) {
            int goal = stateMachine.getGoal(node.state, getRole());
//            System.out.printf("instead of simulation returning goal=%s (state is terminal)\n", goal);
            return goal;
        }

//        long simulateStart = System.currentTimeMillis();
        int[] depthChargeResult = new int[1];
        MachineState finalState = stateMachine.performDepthCharge(node.state, depthChargeResult);
        int goal = stateMachine.getGoal(finalState, getRole());
//        long simulateTime = System.currentTimeMillis() - simulateStart;
//        System.out.printf("simulation done in %s millis\n", simulateTime);
        return goal;
    }

    private Move bestMove(Node node) {
        System.out.println("best move start");
        int maxVisits = 0;
        Move result = null;
        for (Node child : node.children) {
            System.out.printf("  best move child visits/utility = %s/%s\n", child.visits, child.utility);
            if (child.visits > maxVisits) {
                maxVisits = child.visits;
                result = child.moves.get(0);
            }
        }
        System.out.println("best move finished");
        return result;
    }


    private static class Node {
        public final TYPE type;
        public MachineState state;
        public List<Move> moves;
        public int visits = 0;
        public int utility = 0;
        public Node parent;
        public List<Node> children = new ArrayList<Node>();

        public String description;

        private Node(MachineState state, List<Move> moves, Node parent, TYPE type) {
            this.state = state;
            this.moves = moves;
            this.parent = parent;
            this.type = type;

            if (parent == null) {
                this.description = "root";
            } else {
                this.description = parent.description + ":" + parent.children.size();
            }
        }

        public double selectfn() {
            double result = utility/visits;
            if (parent != null) {
                result += sqrt(2*log(parent.visits) / visits);
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
