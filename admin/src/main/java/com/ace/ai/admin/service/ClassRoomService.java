package com.ace.ai.admin.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import com.ace.ai.admin.datamodel.Attendance;
import com.ace.ai.admin.datamodel.Classroom;
import com.ace.ai.admin.datamodel.Student;
import com.ace.ai.admin.datamodel.TeacherBatch;
import com.ace.ai.admin.dtomodel.ClassroomDTO;
import com.ace.ai.admin.dtomodel.ReqClassroomDTO;
import com.ace.ai.admin.repository.AttendanceRepository;
import com.ace.ai.admin.repository.BatchRepository;
import com.ace.ai.admin.repository.ClassRoomRepository;
import com.ace.ai.admin.repository.StudentRepository;
import com.ace.ai.admin.repository.TeacherBatchRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClassRoomService {
    
    @Autowired
    ClassRoomRepository classRoomRepository;

    @Autowired
    TeacherBatchRepository teacherBatchRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    public static String englishTime(String input)
    throws ParseException
{

    // Format of the date defined in the input String
    DateFormat dateFormat
        = new SimpleDateFormat("hh:mm");
   
    // Change the pattern into 24 hour format
    DateFormat format
        = new SimpleDateFormat("HH:mm");
    Date time = null;
    String output = "";
   
    // Converting the input String to Date
    time = dateFormat.parse(input);
   
    // Changing the format of date
    // and storing it in
    // String
    output = format.format(time);
    return output;
}

    public String twelveHourFormat(String time) throws ParseException{

        String format;

        final SimpleDateFormat sdf = new SimpleDateFormat("h:mm");
        final Date dateObj = sdf.parse(time);

        // Parsing hours, minutes and seconds in array
        String[] arr = time.split(":");
 
        // Converting hours into integer
        int hh = Integer.parseInt(arr[0]);
 
        if (hh > 12) {
            hh = hh - 12;
            format = "PM";
        }
        else if (hh == 00) {
            hh = 12;
            format = "AM";
        }
        else if (hh == 12) {
            hh = 12;
            format = "PM";
        }
        else {
            format = "AM";
        }
        log.info(new SimpleDateFormat("hh:mm a").format(dateObj));
        return new SimpleDateFormat("hh:mm a").format(dateObj);
    }

    public List<ClassroomDTO> showClassroomTable(Integer batchId) throws ParseException{

        List<ClassroomDTO> classroomDTOList = new ArrayList<>();
        List<Classroom> classroomList = classRoomRepository.findAllByBatchIdAndDeleteStatus(batchId,false);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dtf
        = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");

        for(Classroom classroom: classroomList){
            ClassroomDTO classroomDTO = new ClassroomDTO();
            classroomDTO.setId(classroom.getId());
            classroomDTO.setDate(LocalDate.parse(classroom.getDate(),df));
            classroomDTO.setLink(classroom.getLink());
            classroomDTO.setRecordedVideo(classroom.getRecordVideo());
            classroomDTO.setStatus("");
            classroomDTO.setTeacherName(classroom.getTeacherName());

            classroomDTO.setStartTime(twelveHourFormat(classroom.getStartTime()));
            classroomDTO.setEndTime(twelveHourFormat(classroom.getEndTime())); 

            classroomDTO.setStartDateTime(LocalDateTime.parse
            (classroomDTO.getDate()+" "+classroom.getStartTime(),dtf));

            classroomDTO.setEndDateTime(LocalDateTime.parse
            (classroomDTO.getDate()+" "+classroom.getEndTime(),dtf));

            if(classroomDTO.getStartDateTime().isAfter(LocalDateTime.now())){
                classroomDTO.setStatus("Upcoming");
            }

            else if(LocalDateTime.now().isAfter
            (classroomDTO.getEndDateTime())){
                classroomDTO.setStatus("Done");
            }

            else if(LocalDateTime.now().isAfter(classroomDTO.getStartDateTime())
            && LocalDateTime.now().isBefore(classroomDTO.getEndDateTime()) ){
                classroomDTO.setStatus("In Progress");
            }

            log.info(classroomDTO.getStatus());
            
            classroomDTOList.add(classroomDTO);
        }
         
        return classroomDTOList;
    } 

    public List<ClassroomDTO> fetchTeacherListForClassroom(Integer batchId){
        List<TeacherBatch> teacherNameList = teacherBatchRepository.findByBatchId(batchId);
        List<ClassroomDTO> classroomDTOforTeacherList = new ArrayList<>();

        for(TeacherBatch teacherBatch : teacherNameList){
            ClassroomDTO classroomDTOforTeacher = new ClassroomDTO();

            classroomDTOforTeacher.setTeacherName(teacherBatch.getTeacher().getName());
            classroomDTOforTeacher.setBatchId(batchId);
            classroomDTOforTeacherList.add(classroomDTOforTeacher);
        }

        log.info(classroomDTOforTeacherList.get(0).getTeacherName());
        return classroomDTOforTeacherList;
    }

    public void createClassroom(ReqClassroomDTO reqClassroomDTO) throws ParseException{
        Classroom classroom = new Classroom();

        classroom.setDate(reqClassroomDTO.getDate());
        classroom.setLink(reqClassroomDTO.getLink());
        classroom.setBatch(batchRepository.findBatchById(reqClassroomDTO.getBatchId()));
        classroom.setTeacherName(reqClassroomDTO.getTeacherName());
        classroom.setStartTime(englishTime(reqClassroomDTO.getStartTime()));
        classroom.setEndTime(englishTime(reqClassroomDTO.getEndTime()));
        classRoomRepository.save(classroom);
        createAttendanceByClassroom(classroom);
    }

    public void createAttendanceByClassroom(Classroom classroom){

        Attendance attendance = new Attendance();
        attendance.setClassroom(classroom);

        List<Student>students = studentRepository.findByBatch(classroom.getBatch());

        for(Student student : students){
            attendance.setStudent(student);
            attendance.setAttend("Present");
            attendanceRepository.save(attendance);
        }
    }

    public Classroom fetchClassroom(ClassroomDTO classroomDTO){
        Classroom classroom = classRoomRepository.findById(classroomDTO.getId()).get();
        return classroom;
    }
}
