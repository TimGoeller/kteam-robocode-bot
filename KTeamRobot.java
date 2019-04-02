 package kteam;

import robocode.*;
import robocode.Robot;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class KTeamRobot extends AdvancedRobot {

    int[][] defaultDangerGrid;
    int[][] currentDangerGrid;

    Point enemyPosition;

    Point currentGoal;

    Point myPositon;

    int lastDangerLevel = 0;

    int reevaluationCounter = 10;

    List<Point> lastLowestCrossingDangerPointsDebug;

    boolean initFinished = false;
    @Override
    public void run() {
        super.run();

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setBodyColor(Color.red);
        setGunColor(Color.green);
        setRadarColor(Color.blue);

        initializeDefaultGrid();
        initFinished = true;
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
        do {
            scan();
        } while (true);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        if(initFinished) {
            double radarTurn =
                    // Absolute bearing to target
                    getHeadingRadians() + event.getBearingRadians()
                            // Subtract current radar heading to get turn required
                            - getRadarHeadingRadians();

            setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));

            double absoluteBearing = getHeadingRadians() + event.getBearingRadians();
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle(absoluteBearing -
                            getGunHeadingRadians()));

            double angleToEnemy = Math.toRadians((getHeading() + event.getBearing()) % 360);
            enemyPosition = new Point((int)(getX() + Math.sin(angleToEnemy) * event.getDistance()), (int)(getY() + Math.cos(angleToEnemy) * event.getDistance()));

            myPositon = new Point((int)getX(), (int)getY());
            reevaluationCounter--;
            if(reevaluationCounter == 0) {
                updateGrid();
                if(currentGoal != null) {
                    if(currentDangerGrid[currentGoal.x][currentGoal.y] > lastDangerLevel || Math.abs(myPositon.x - currentGoal.x) < 15 && Math.abs(myPositon.y - currentGoal.y) < 15) {
                        currentGoal = getRandomPerfectPosition();
                        System.out.println(currentGoal);
                    }
                }
                else {
                    currentGoal = getRandomPerfectPosition();
                    System.out.println(currentGoal);
                }
                reevaluationCounter = 15;
                lastDangerLevel = currentDangerGrid[currentGoal.x][currentGoal.y];
            }
            if(currentGoal != null) {
                goTo(currentGoal.x, currentGoal.y);
            }
        }

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10)
            setFire(2);
    }

    private void goTo(int x, int y) {
        double a;
        setTurnRightRadians(Math.tan(
                a = Math.atan2(x -= (int) getX(), y -= (int) getY())
                        - getHeadingRadians()));
        setAhead(Math.hypot(x, y) * Math.cos(a));
    }

    private void initializeDefaultGrid() {
        defaultDangerGrid = new int[(int)getBattleFieldWidth()][(int)getBattleFieldHeight()];

        addDefaultGridBorder((int)(defaultDangerGrid.length * 0.10), (int)(defaultDangerGrid[0].length * 0.07), 1);
        addDefaultGridBorder((int)(defaultDangerGrid.length * 0.05), (int)(defaultDangerGrid[0].length * 0.03), 2);
    }

    private void addDefaultGridBorder(int xLength, int yLength, int danger) {
        for(int x = 0; x < xLength; x++) {
            for(int y = 0; y < defaultDangerGrid[0].length; y++) {
                defaultDangerGrid[x][y] += danger;
            }
        }

        for(int x = defaultDangerGrid.length - 1; x > defaultDangerGrid.length - xLength - 1; x--) {
            for(int y = 0; y < defaultDangerGrid[0].length; y++) {
                defaultDangerGrid[x][y] += danger;
            }
        }

        for(int x = 0; x < defaultDangerGrid.length; x++) {
            for(int y = 0; y < yLength; y++) {
                defaultDangerGrid[x][y] += danger;
            }
        }

        for(int x = 0; x < defaultDangerGrid.length; x++) {
            for(int y = defaultDangerGrid[0].length - 1; y > defaultDangerGrid[0].length - yLength - 1; y--) {
                defaultDangerGrid[x][y] += danger;
            }
        }
    }

    private Point getRandomPerfectPosition() {
        int lowestDanger = 50;
        for(int x = 0; x < currentDangerGrid.length; x++) {
            for(int y = 0; y < currentDangerGrid[0].length; y++) {
                if(currentDangerGrid[x][y] < lowestDanger) {
                    lowestDanger = currentDangerGrid[x][y];
                }
            }
        }

        List<Point> perfectPositons = new ArrayList<>();
        for(int x = 0; x < currentDangerGrid.length; x++) {
            for(int y = 0; y < currentDangerGrid[0].length; y++) {
                if(currentDangerGrid[x][y] == lowestDanger) {
                    perfectPositons.add(new Point(x,y));
                }
            }
        }

        int lowestCostSum = 50;
        List<Point> lowestCostSumPoints = new ArrayList<>();

        for (Point position : perfectPositons) {
            Point vectorBetweenMeAndPositon = new Point(position.x - myPositon.x, position.y - myPositon.y);
            if(Math.hypot(vectorBetweenMeAndPositon.x, vectorBetweenMeAndPositon.y) <  10) {
                continue;
            }
            //int highestLocalCrossingDanger = 0;
            int costSum = 0;
            for(int i = 1; i <= 10; i++) {
                /*Point pointOnLineToCheck = new Point((int)(myPositon.x + vectorBetweenMeAndPositon.x * (i * 0.1)), (int)(myPositon.y + vectorBetweenMeAndPositon.y * (i * 0.1)));
                if(currentDangerGrid[pointOnLineToCheck.x][pointOnLineToCheck.y] > highestLocalCrossingDanger) {
                    highestLocalCrossingDanger = currentDangerGrid[pointOnLineToCheck.x][pointOnLineToCheck.y];
                }*/
                Point pointOnLineToCheck = new Point((int)(myPositon.x + vectorBetweenMeAndPositon.x * (i * 0.1)), (int)(myPositon.y + vectorBetweenMeAndPositon.y * (i * 0.1)));
                costSum += currentDangerGrid[pointOnLineToCheck.x][pointOnLineToCheck.y];
            }

            //Add values???

            if(costSum < lowestCostSum) {
                lowestCostSum = costSum;
                lowestCostSumPoints = new ArrayList<>();
            }
            else if(costSum == lowestCostSum) {
                lowestCostSumPoints.add(position);
            }
        }

        lastLowestCrossingDangerPointsDebug = lowestCostSumPoints;
        return lowestCostSumPoints.get((int)(Math.random() * (lowestCostSumPoints.size() - 1)));
    }

    private void updateGrid() {
        resetCurrentGrid();
        if(enemyPosition != null) {

            int collisionZoneRadiusSquared = (int)Math.pow(50, 2);
            for(int x = 0; x < currentDangerGrid.length; x++) {
                for(int y = 0; y < currentDangerGrid[0].length; y++) {
                    if((Math.pow((x - enemyPosition.x),2) + Math.pow((y - enemyPosition.y),2)) < collisionZoneRadiusSquared) {
                        currentDangerGrid[x][y] += 2;
                    }
                }
            }

            int innerCircleRadiusSquared = (int)Math.pow(150, 2);
            for(int x = 0; x < currentDangerGrid.length; x++) {
                for(int y = 0; y < currentDangerGrid[0].length; y++) {
                    if((Math.pow((x - enemyPosition.x),2) + Math.pow((y - enemyPosition.y),2)) < innerCircleRadiusSquared) {
                        currentDangerGrid[x][y] += 2;
                    }
                }
            }

            int outerCircleRadiusSquared = (int)Math.pow(350, 2);
            for(int x = 0; x < currentDangerGrid.length; x++) {
                for(int y = 0; y < currentDangerGrid[0].length; y++) {
                    if((Math.pow((x - enemyPosition.x),2) + Math.pow((y - enemyPosition.y),2)) < outerCircleRadiusSquared) {
                        currentDangerGrid[x][y] += 1;
                    }
                }
            }
        }
    }

    public void onPaint(Graphics2D g) {
        if(currentDangerGrid != null)
        for(int x = 0; x < currentDangerGrid.length; x++) {
            for(int y = 0; y < currentDangerGrid[0].length; y++) {
                g.setColor(new Color(20 * currentDangerGrid[x][y], 255 - 20 *  currentDangerGrid[x][y], 0, 255));
                g.fillRect(x, y, 1, 1);
            }
        }
        g.setColor(Color.BLACK);
        for(Point p : lastLowestCrossingDangerPointsDebug) {
            g.drawLine(myPositon.x, myPositon.y, p.x, p.y);
        }

    }

    private void resetCurrentGrid() {
        currentDangerGrid = new int[defaultDangerGrid.length][defaultDangerGrid[0].length];
        for(int i = 0; i < defaultDangerGrid.length; i++)
        {
            int[] aMatrix = defaultDangerGrid[i];
            int   aLength = aMatrix.length;
            currentDangerGrid[i] = new int[aLength];
            System.arraycopy(aMatrix, 0, currentDangerGrid[i], 0, aLength);
        }
    }
}
