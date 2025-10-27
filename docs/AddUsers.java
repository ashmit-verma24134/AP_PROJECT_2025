//package edu.univ.erp.ui.admin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AddUsers {
    public static void main(String[] agrs){
        JFrame frame = new JFrame("Add Users");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        Jlabel heading = new JLabel("Add New User");
        heading.setFont(new Font("Serif", Font.BOLD, 20));
        heading.setBounds(130, 10, 200, 30);
        frame.add(heading);

        Jlabel user_heading = new JLabel("Username:");
        user_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //username ka field add ho gaya
        user_heading.setBounds(50, 60, 100, 25);
        frame.add(user_heading);

        us_n = new JTextField();
        us_n.setBounds(150, 60, 200, 25);
        frame.add(us_n);

        Jlabel pw_heading = new JLabel("Password:");
        pw_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //password ka field add ho gaya
        pw_heading.setBounds(50, 100, 100, 25);
        frame.add(pw_heading);

        pw = new JPasswordField();
        pw.setBounds(150, 100, 200, 25);
        frame.add(pw);

        Jlabel role = new JLabel("Role:");
        role.setFont(new Font("Serif", Font.PLAIN, 16));        //role ka field add ho gaya
        role.setBounds(50, 140, 100, 25);
        frame.add(role);


        String[] roles = { "Student", "Instructor", "Admin" };
        JComboBox<String> roleList = new JComboBox<>(roles);
        roleList.setBounds(150, 140, 200, 25);
        frame.add(roleList);

        Jlabel student_roll_heading = new JLabel("Student Roll No:");
        student_roll_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //student roll number ka field add ho gaya
        student_roll_heading.setBounds(50, 180, 120, 25);
        frame.add(student_roll_heading);

        student_roll_num = new JTextField();
        student_roll_num.setBounds(180, 180, 170, 25);
        frame.add(student_roll_num);

        Jlabel program_heading = new JLabel("Program:");
        program_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //program ka field add ho gaya
        program_heading.setBounds(50, 220, 100, 25);
        frame.add(program_heading);

        program = new JTextField();
        program.setBounds(150, 220, 200, 25);
        frame.add(program);

        Jlabel year_heading = new JLabel("Year:");
        year_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //year ka field add ho gaya
        year_heading.setBounds(50, 260, 100, 25);
        frame.add(year_heading);

        year = new JTextField();
        year.setBounds(150, 260, 200, 25);
        frame.add(year);

        Jlabel instr_dept_heading = new JLabel("Instructor Dept:");
        instr_dept_heading.setFont(new Font("Serif", Font.PLAIN, 16));        //instructor department ka field add ho gaya
        instr_dept_heading.setBounds(50, 300, 130, 25);
        frame.add(instr_dept_heading);

        instr_dept = new JTextField();
        instr_dept.setBounds(180, 300, 170, 25);
        frame.add(instr_dept);

        Jbutton addButton = new JButton("Add User");
        addButton.setBounds(150, 340, 100, 30);
        frame.add(addButton);

        roleList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedRole = (String) roleList.getSelectedItem();
                if (selectedRole.equals("Student")) {
                    student_roll_heading.setVisible(true);
                    student_roll_num.setVisible(true);
                    program_heading.setVisible(true);
                    program.setVisible(true);
                    year_heading.setVisible(true);
                    year.setVisible(true);
                    instr_dept_heading.setVisible(false);
                    instr_dept.setVisible(false);
                } else if (selectedRole.equals("Instructor")) {
                    student_roll_heading.setVisible(false);
                    student_roll_num.setVisible(false);
                    program_heading.setVisible(false);
                    program.setVisible(false);
                    year_heading.setVisible(false);
                    year.setVisible(false);
                    instr_dept_heading.setVisible(true);
                    instr_dept.setVisible(true);
                } else {
                    student_roll_heading.setVisible(false);
                    student_roll_num.setVisible(false);
                    program_heading.setVisible(false);
                    program.setVisible(false);
                    year_heading.setVisible(false);
                    year.setVisible(false);
                    instr_dept_heading.setVisible(false);
                    instr_dept.setVisible(false);
                }
            }
        });


        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);
        
        frame.setVisible(true);

    }
    
}
