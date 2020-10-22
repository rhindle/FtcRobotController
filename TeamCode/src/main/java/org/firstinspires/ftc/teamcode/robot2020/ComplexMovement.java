package org.firstinspires.ftc.teamcode.robot2020;

import android.database.Cursor;

import androidx.room.Query;
import androidx.room.Room;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.robot2020.persistence.AppDatabase;
import org.firstinspires.ftc.teamcode.robot2020.persistence.MovementEntity;
import org.firstinspires.ftc.teamcode.robot2020.persistence.MovementEntityDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.sleep;

@Config
public class ComplexMovement {

    //////////////////
    //user variables//
    //////////////////
    //make
    protected String dataBaseName = "FIRST_INSPIRE_2020";
    public static double measureDelay = 0; //in ms
    public static double maxTime = 1350 * 3; //in ms
    public static double timePerLoop = 13.5; //in ms
    //load
    public static double load_TimePerLoop = 10.5; //in ms
    //other
    public static double maxVelocity = 537.6 * 312; //in ticks per second

    ///////////////////
    //other variables//
    ///////////////////
    protected AppDatabase db;
    //make
    protected List<int[]> positions = new ArrayList<>();
    protected List<double[]> velocities = new ArrayList<>();
    protected boolean isRecording = false;
    protected double curRecordingLength = 0;
    protected boolean startOfRecording = true;
    protected int[] motorStartOffset;
    //load
    protected List<int[]> loaded_Positions = new ArrayList<>();
    protected List<double[]> loaded_Velocities = new ArrayList<>();
    protected double loaded_TotalMeasureDelay;
    protected double loaded_TotalTime;

    //other class objects
    Robot robot;

    ComplexMovement(Robot robot)
    {
        this.robot = robot;
        initComplexMovement();
    }

    void initComplexMovement()
    {
        db = Room.databaseBuilder(AppUtil.getDefContext(), AppDatabase.class, dataBaseName).build();
    }

    void recorder()
    {
        if (measureDelay > maxTime || (measureDelay == 0 && timePerLoop == 0) && robot.debug_methods) robot.addTelemetryString("error in ComplexMovement.recorder: ", "measure delay is more than max length so can not record");
        if(isRecording)
        {
            if(curRecordingLength + measureDelay + timePerLoop > maxTime)
            {
                if(robot.debug_methods)robot.addTelemetryString("ComplexMovement.recorder has stopped recording: ", "this recording has stopped at time " + curRecordingLength + " ms: stop recording to make file");
                isRecording = false;
            }
            else
            {
                if(startOfRecording)
                {
                    motorStartOffset = robot.motorConfig.getMotorPositionsList(robot.motorConfig.driveMotors);
                    startOfRecording = false;
                }

                int[] pos = robot.motorConfig.getMotorPositionsList(robot.motorConfig.driveMotors);
                for(int i = 0; i < pos.length; i++) pos[i] -= motorStartOffset[i];
                positions.add(pos);

                velocities.add(robot.motorConfig.getMotorVelocitiesList(robot.motorConfig.driveMotors));

                curRecordingLength += measureDelay + timePerLoop;
            }
            if(measureDelay > 0) sleep((long)measureDelay);
        }
    }
    void startRecording(boolean reset)
    {
        if (reset) resetRecording();
        isRecording = true;
    }

    void pauseRecording()
    {
        isRecording = false;
    }

    void stopRecording(boolean makeFile, String moveName)
    {
        if(makeFile)
        {
            makeFile(moveName);
        }
        isRecording = false;
    }

    void resetRecording()
    {
        positions.clear();
        velocities.clear();
        curRecordingLength = 0;
        startOfRecording = true;
        motorStartOffset = null;
    }

    void makeFile(String moveName)
    {
        if (moveName == null || moveName.equals("")) moveName = "not named";
        MovementEntity entity = new MovementEntity(moveName, 0, (int) maxTime, measureDelay);
        db.movementEntityDAO().insertAll(entity);
        entity = new MovementEntity(moveName, 0, (int) curRecordingLength, timePerLoop);
        db.movementEntityDAO().insertAll(entity);
        for (int i = 0; i < positions.size(); i++)
        {
            for (int m = 0; m < robot.motorConfig.driveMotors.size(); m++) {
                MovementEntity entity1 = new MovementEntity(moveName, m + 1, positions.get(i)[m], velocities.get(i)[m]);
                db.movementEntityDAO().insertAll(entity1);
            }
        }
    }

    void loadMoveDB(String moveName)
    {
        List<MovementEntity> data = db.movementEntityDAO().loadMovementByName(moveName);
        int[] currentLoadTick = new int[4];
        double[] currentLoadVelocity = new double[4];
        int i = 0;
        loaded_TotalMeasureDelay = 0;

        for(MovementEntity m:data)
        {
            if(m.motor_id == 0)
            {
                if(i == 0)loaded_TotalMeasureDelay += m.motor_velocity;
                else
                {
                    loaded_TotalTime = m.motor_tick;
                    loaded_TotalMeasureDelay += m.motor_velocity;
                }
                i++;
            }
            else
            {
                currentLoadTick[m.motor_id - 1] = m.motor_tick;
                currentLoadVelocity[m.motor_id - 1] = m.motor_velocity;
                if(m.motor_id == 4)
                {
                    loaded_Positions.add(currentLoadTick);
                    loaded_Velocities.add(currentLoadVelocity);
                    currentLoadTick = new int[4];
                    currentLoadVelocity = new double[4];
                }
            }
        }
    }

    void loadMoveCSV(String fileName)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("assets/" + fileName)));
            String Line;
            int[] currentLoadTick = new int[4];
            double[] currentLoadVelocity = new double[4];
            int i = 0;
            loaded_TotalMeasureDelay = 0;

            while ((Line = reader.readLine()) != null)
            {
                String[] elements = Line.split(",");
                if(Integer.parseInt(elements[1]) == 0)
                {
                    if(i == 0) loaded_TotalMeasureDelay += Double.parseDouble(elements[3]);
                    else
                    {
                        loaded_TotalTime = Double.parseDouble(elements[2]);
                        loaded_TotalMeasureDelay += Double.parseDouble(elements[3]);
                    }
                    i++;
                }
                else
                {
                    currentLoadTick[currentLoadTick.length] = Integer.parseInt(elements[2]);
                    currentLoadVelocity[currentLoadVelocity.length] = Double.parseDouble(elements[3]);
                    if(Integer.parseInt(elements[1]) == 4)
                    {
                        loaded_Positions.add(currentLoadTick);
                        loaded_Velocities.add(currentLoadVelocity);
                        currentLoadTick = new int[4];
                        currentLoadVelocity = new double[4];
                    }
                }
            }
        }
        catch (IOException e)
        {
            if(robot.debug_methods)robot.addTelemetryString("error", e.toString());
        }
    }

    void scaleLoadedMove(boolean scaleToMaxPower)
    {
        double maxMeasuredVelocity = 0;
        for(double[] line:loaded_Velocities)
        {
            for(double value:line) if(value > maxMeasuredVelocity) maxMeasuredVelocity = value;
        }
        if(maxMeasuredVelocity > maxVelocity || scaleToMaxPower)
        {
            double multiplier = maxVelocity/maxMeasuredVelocity;
            loaded_TotalTime /= multiplier;
            loaded_TotalMeasureDelay /= multiplier;

            for(int l = 0; l < loaded_Velocities.size(); l++)
            {
                for(int v = 0; v < loaded_Velocities.get(0).length; v++)
                {
                    loaded_Velocities.get(l)[v] *= multiplier;
                    loaded_Positions.get(l)[v] *= multiplier;
                }
            }
        }
    }

    void clearMove()
    {
        loaded_Positions.clear();
        loaded_Velocities.clear();
        loaded_TotalMeasureDelay = 0;
        loaded_TotalTime = 0;
    }

    void runMove(double speedMultiplier)
    {
        robot.motorConfig.setMotorsToCoastList(robot.motorConfig.driveMotors);
        double timesToRun = (loaded_TotalMeasureDelay - load_TimePerLoop)/load_TimePerLoop;
        double totalDelay = loaded_TotalMeasureDelay + load_TimePerLoop;
        
        if(timesToRun < 1)
        {
            timesToRun = 1;
            if(robot.debug_methods) robot.addTelemetryString("error in method ComplexMovement.runMove: ", "the time it take to calculate and set motor velocities may be more than the time interval between measuring, this could cause problems");
        }

        double[] calculatedVelocity = new double[loaded_Velocities.get(0).length];

        for(int i = 0; i < loaded_Velocities.size(); i++)
        {
            for(int l = 0; l < timesToRun; l++)
            {
                for(int m = 0; m < calculatedVelocity.length; m++)
                {
                    calculatedVelocity[m] = ((loaded_Velocities.get(i + 1)[m] - loaded_Velocities.get(i)[m]) / totalDelay) * (l * load_TimePerLoop);
                }
            }
        }
        robot.motorConfig.setMotorsToCoastList(robot.motorConfig.driveMotors);
    }

    void runMoveV2(double speedMultiplier)
    {
        robot.motorConfig.setMotorsToCoastList(robot.motorConfig.driveMotors);
        double delay = (loaded_TotalMeasureDelay/speedMultiplier) - load_TimePerLoop;
        double totalTimePerLoop = delay + 2*load_TimePerLoop;

        if(delay < 0)
        {
            delay = 0;
            if(robot.debug_methods) robot.addTelemetryString("warning in method ComplexMovement.runMoveV2: ", "the time it take to calculate and set motor velocities may be more than the time interval between measuring or your speed multiplier may be to high, this could cause problems");
        }

        double[] calculatedPower = new double[loaded_Velocities.get(0).length];
        int[] motorStartPosition = robot.motorConfig.getMotorPositionsList(robot.motorConfig.driveMotors);
        int[] calculatedPosition = new int[loaded_Velocities.get(0).length];
        double currentTime = 0;

        for(int i = 0; i < loaded_Velocities.size(); i++)
        {
            currentTime += totalTimePerLoop;
            if(currentTime > loaded_TotalTime)
            {
                if(robot.debug_methods) robot.addTelemetryString("error in method ComplexMovement.runMoveV2: ", "running this move took longer than expected, exiting move...");
                break;
            }
            for(int m = 0; m < calculatedPower.length; m++)
            {
                calculatedPosition[m] = motorStartPosition[m] + loaded_Positions.get(i)[m];
                calculatedPower[m] = loaded_Velocities.get(i)[m]/maxVelocity * speedMultiplier;
            }
            robot.motorConfig.setMotorsToSeparatePositionsAndPowersList(robot.motorConfig.driveMotors, calculatedPosition, calculatedPower);
            sleep((long)delay);
            if(robot.stop()) break;
        }
        robot.motorConfig.setMotorsToBrakeList(robot.motorConfig.driveMotors);
    }
}