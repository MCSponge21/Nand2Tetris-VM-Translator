import java.util.*;
import java.io.*;

public class VM {

    private static int mJumpNumber = 0;
    private static String currentFunctionName = "";
    private static int currentReturnAddressN = 0;
    private static String currentClassName;

    private enum Command_Type{
        C_ARITHMETIC,
        C_PUSH,
        C_POP,
        C_LABEL,
        C_GOTO,
        C_IF,
        C_FUNCTION,
        C_RETURN,
        C_CALL
    }
    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator <.vm file or directory>");
            return;
        }

        String inputPath = args[0];
        File inputFile = new File(inputPath);

        String outputAsmFilePath = "";
        List<File> vmFilesToTranslate = new ArrayList<>();


        try {
            if (inputFile.isFile() && inputPath.endsWith(".vm")) {
                outputAsmFilePath = inputPath.substring(0, inputPath.lastIndexOf('.')) + ".asm";
                vmFilesToTranslate.add(inputFile);

            } else if (inputFile.isDirectory()) {
                outputAsmFilePath = inputPath + File.separator + inputFile.getName() + ".asm";

                File[] filesInDir = inputFile.listFiles((dir, name) -> name.endsWith(".vm"));
                if (filesInDir != null) {
                    Arrays.sort(filesInDir, (f1, f2) -> {
                        if ("Sys.vm".equals(f1.getName())) return -1;
                        if ("Sys.vm".equals(f2.getName())) return 1;
                        return f1.getName().compareTo(f2.getName());
                    });
                    vmFilesToTranslate.addAll(Arrays.asList(filesInDir));
                }
            } else {
                System.out.println("Invalid input: must be a .vm file or a directory.");
                return;
            }

            try (PrintWriter outputWriter = new PrintWriter(outputAsmFilePath)) {

                outputWriter.println(writeBootstrap());

                for (File vmFile : vmFilesToTranslate) {
                    String className = vmFile.getName().replace(".vm", "");
                    processFile(vmFile.getAbsolutePath(), outputWriter, className);
                }
            } 

            System.out.println("Translation complete! Output file: " + outputAsmFilePath);

        } catch (IOException e) {
            System.err.println("Error during translation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private static void processFile(String file, PrintWriter writer, String classname){
        currentClassName = classname;

        try{
            FileReader vmFile = new FileReader(file);
            Scanner scanner = new Scanner(vmFile);

            while(scanner.hasNextLine()){
                String line = scanner.nextLine().trim();
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                String code = processLine(line);
                writer.println("//"+line);
                writer.println(code);
            }

            scanner.close();
        }catch(IOException e){

        }
    }

    private static String processLine(String line){
        String code = "";
        String[] parts;
        Command_Type command_Type = getCommandType(line);
        switch (command_Type) {
            case C_PUSH:
                parts = line.split(" ");
                return writePushPop(true, parts[1], parts[2]);
            case C_POP:
                parts = line.split(" ");
                return writePushPop(false, parts[1], parts[2]);
            case C_ARITHMETIC:
                return writeArithmetic(line);
            case C_LABEL:
                return writeLabel(line);
            case C_IF:
                return writeIf(line);
            case C_GOTO:
                return writeGoto(line);
            case C_FUNCTION:
                return writeFunction(line);
            case C_CALL:
                return writeCall(line);
            case C_RETURN:
                return writeReturn(line);
            default:
                break;
        }
        return code;
    }

    private static String writeBootstrap(){
        String code = "@256\nD=A\n@0\nM=D\n";
        return code;
    }

    private static String writeReturn(String line){
        String code = "";
        code = "@LCL\nD=M\n@R13\nM=D\n" +
                "@5\nA=D-A\nD=M\n@R14\nM=D\n@SP\nM=M-1\nA=M\nD=M\n@ARG\nA=M\nM=D\n"+
                "@ARG\nD=M\n@SP\nM=D+1\n"+
                "@R13\nM=M-1\nA=M\nD=M\n@THAT\nM=D\n"+
                "@R13\nM=M-1\nA=M\nD=M\n@THIS\nM=D\n"+
                "@R13\nM=M-1\nA=M\nD=M\n@ARG\nM=D\n"+
                "@R13\nM=M-1\nA=M\nD=M\n@LCL\nM=D\n"+
                "@R14\nA=M\n0;JMP\n";
        return code;
    }

    private static String writeCall(String line){
        String code = "";
        String[] parts = line.split(" ");
        code = "@"+currentFunctionName+"$ret_"+currentReturnAddressN + "\n" + 
                "D=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n" + 
                "@LCL\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n" + 
                "@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n" +
                "@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n" + 
                "@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n" +
                "@SP\nD=M\n@5\nD=D-A\n@"+parts[2]+"\nD=D-A\n@ARG\nM=D\n"+
                "@SP\nD=M\n@LCL\nM=D\n"+
                "@"+parts[1]+"\n0;JMP\n"+
                "("+currentFunctionName+"$ret_" + currentReturnAddressN + ")\n";
        currentReturnAddressN++;
        return code;
    }

    private static String writeFunction(String line){
        String code = "";
        String[] parts = line.split(" ");
        currentFunctionName = parts[1];

        code = "(" + parts[1]+")\n@"+parts[2]+"\nD=A\n@R13\nM=D\n@"+parts[1]+"$END_INIT_LOCALS\nD;JEQ\n("+parts[1]+"$init_loop)\n"+
                "@SP\nA=M\nM=0\n@SP\nM=M+1\n@R13\nM=M-1\nD=M\n@"+parts[1]+"$init_loop\nD;JGT\n("+parts[1]+"$END_INIT_LOCALS)\n";

        return code;
    }

    private static String writeGoto(String line){
        String code = "";
        String[] parts = line.split(" ");

        code = "@"+parts[1]+"\n0;JMP\n";

        return code;
    }

    private static String writeIf(String line){
        String code = "";
        String[] parts = line.split(" ");

        code = "@SP\nM=M-1\nA=M\nD=M\n@"+parts[1]+"\nD;JNE\n";
        return code;
    }

    private static String writeLabel(String line){
        String code = "";
        String[] parts = line.split(" ");

        code = "("+parts[1]+")\n";
        return code;
    }

    private static String writeArithmetic(String operation){
        String code = "";
        if(operation.equals("add")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D+M\n@SP\nM=M+1\n";
        }else if(operation.equals("sub")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=M-D\n@SP\nM=M+1\n";
        }else if(operation.equals("neg")){
            code = "@SP\nM=M-1\nA=M\nM=-M\n@SP\nM=M+1\n";
        }else if(operation.equals("eq")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nD=M-D\n@FALSE" + mJumpNumber + "\nD;JNE\n@SP\nA=M\nM=-1\n@CONTINUE" + mJumpNumber + "\n0;JMP\n(FALSE" + mJumpNumber + ")\n@SP\nA=M\nM=0\n(CONTINUE" + mJumpNumber + ")\n@SP\nM=M+1\n";
            mJumpNumber++;
        }else if(operation.equals("gt")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nD=M-D\n@FALSE" + mJumpNumber + "\nD;JLE\n@SP\nA=M\nM=-1\n@CONTINUE" + mJumpNumber + "\n0;JMP\n(FALSE" + mJumpNumber + ")\n@SP\nA=M\nM=0\n(CONTINUE" + mJumpNumber + ")\n@SP\nM=M+1\n";
            mJumpNumber++;
        }else if(operation.equals("lt")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nD=M-D\n@FALSE" + mJumpNumber + "\nD;JGE\n@SP\nA=M\nM=-1\n@CONTINUE" + mJumpNumber + "\n0;JMP\n(FALSE" + mJumpNumber + ")\n@SP\nA=M\nM=0\n(CONTINUE" + mJumpNumber + ")\n@SP\nM=M+1\n";
            mJumpNumber++;
        }else if(operation.equals("and")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D&M\n@SP\nM=M+1\n";
        }else if(operation.equals("or")){
            code = "@SP\nM=M-1\nA=M\nD=M\n@SP\nM=M-1\nA=M\nM=D|M\n@SP\nM=M+1\n";
        }else if(operation.equals("not")){
            code = "@SP\nM=M-1\nA=M\nM=!M\n@SP\nM=M+1\n";
        }
        return code;
    }

    private static String writePushPop(boolean isPush, String segment, String num){
        String code = "";
        if(isPush){
            if(segment.equals("constant")){
                code = "@"+num+"\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("local")){
                code = "@LCL\nD=M\n@"+num+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("argument")){
                code = "@ARG\nD=M\n@"+num+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("this")){
                code = "@THIS\nD=M\n@"+num+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("that")){
                code = "@THAT\nD=M\n@"+num+"\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("static")){
                code = "@"+currentClassName+"."+num+"\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }else if(segment.equals("pointer")){
                if(Integer.parseInt(num) == 0){
                    code = "@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                    return code;
                }else{
                    code = "@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                    return code;
                }
            }
            else if(segment.equals("temp")){
                code = "@5\n@" + num + "\nD=A\n@5\nA=D+A\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
                return code;
            }
        }else{
            if(segment.equals("local")){
                code = "@LCL\nD=M\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }else if(segment.equals("argument")){
                code = "@ARG\nD=M\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }else if(segment.equals("this")){
                code = "@THIS\nD=M\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }else if(segment.equals("that")){
                code = "@THAT\nD=M\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }else if(segment.equals("static")){
                code = "@SP\nM=M-1\nA=M\nD=M\n@"+currentClassName+"."+num+"\nM=D\n";
                return code;
            }else if(segment.equals("pointer")){
                code = "@3\nD=A\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nM=M-1\nA=M\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }
            else if(segment.equals("temp")){
                code = "@5\nD=A\n@"+num+"\nD=D+A\n@R13\nM=D\n@SP\nM=M-1\nA=M\nD=M\n@R13\nA=M\nM=D\n";
                return code;
            }
        }
        return code;
    }

    private static Command_Type getCommandType(String line){
        if(line.startsWith("push")){
            return Command_Type.C_PUSH;
        }else if(line.startsWith("pop")){
            return Command_Type.C_POP;
        }else if(line.startsWith("add") || line.startsWith("sub") || line.startsWith("neg") || line.startsWith("eq") 
        || line.startsWith("gt") || line.startsWith("lt") || line.startsWith("and") || line.startsWith("or") 
        || line.startsWith("not")){
            return Command_Type.C_ARITHMETIC;
        }else if(line.startsWith("label")){
            return Command_Type.C_LABEL;
        }else if(line.startsWith("if-goto")){
            return Command_Type.C_IF;
        }else if(line.startsWith("goto")){
            return Command_Type.C_GOTO;
        }else if(line.startsWith("function")){
            return Command_Type.C_FUNCTION;
        }else if(line.startsWith("call")){
            return Command_Type.C_CALL;
        }else{
            return Command_Type.C_RETURN;
        }
    }
}