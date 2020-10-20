package org.firstinspires.ftc.teamcode.robot2020;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

// test
@Config
@TeleOp(name = "test launcher read")
public class Test extends LinearOpMode
{

    Robot robot;


    @Override
    public void runOpMode()
    {
        robot = new Robot(hardwareMap,telemetry,gamepad1,gamepad2,false, true, false, false, false);
        robot.motorConfig.setDriveMotorsToCoastList(robot.motorConfig.driveMotors);
        robot.startTelemetry();
        robot.complexMovement.startRecording(true);
        robot.sendTelemetry();

        waitForStart();

        while (opModeIsActive())
        {

            while(robot.complexMovement.isRecording && opModeIsActive())
            {
                robot.startTelemetry();
                robot.complexMovement.recorder();
                robot.addTelemetryDouble("size", robot.complexMovement.positions.size());
                robot.addTelemetryDouble("size 2", robot.complexMovement.velocities.size());
                robot.sendTelemetry();
            }
            robot.complexMovement.stopRecording(true, "test");
            robot.sendTelemetry();
            break;
        }
    }
}
