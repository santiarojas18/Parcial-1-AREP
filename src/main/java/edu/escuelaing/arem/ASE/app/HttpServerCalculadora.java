package edu.escuelaing.arem.ASE.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpServerCalculadora {
    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(45000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 45000.");
            System.exit(1);
        }

        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            String inputLine, outputLine;
            boolean firstLine = true;
            String uri = "";

            while ((inputLine = in.readLine()) != null) {
                if (firstLine) {
                    uri = inputLine.split(" ")[1];
                    firstLine = false;
                    System.out.println("THE URI IS: " + uri);
                }

                System.out.println("Recibí: " + inputLine);
                if (!in.ready()) {break; }
            }

            URI uriObject = new URI(uri);
            String answer = "";

            if (uri.startsWith("/compreflex")) {
                String methodToCall = uriObject.getQuery().replace("comando=", "");
                System.out.println(methodToCall);
                String parametersFromMehtod = null;
                String[] finalParameters;
                if (methodToCall.startsWith("binaryInvoke")) {
                    parametersFromMehtod = methodToCall.substring(13,methodToCall.length()-1);
                    finalParameters = separateParameters(parametersFromMehtod);
                    answer = executeBinary(finalParameters);
                } else if (methodToCall.startsWith("class")) {
                    parametersFromMehtod = methodToCall.substring(6,methodToCall.length()-1);
                    finalParameters = separateParameters(parametersFromMehtod);
                } else if (methodToCall.startsWith("invoke")) {
                    parametersFromMehtod = methodToCall.substring(7,methodToCall.length()-1);
                    finalParameters = separateParameters(parametersFromMehtod);
                    answer = executeInvoke(finalParameters);
                } else if (methodToCall.startsWith("unaryInvoke")) {
                    parametersFromMehtod = methodToCall.substring(12,methodToCall.length()-1);
                    finalParameters = separateParameters(parametersFromMehtod);
                    answer = executeUniray(finalParameters);
                }

            }
            outputLine = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html\r\n"
                    + "\r\n"
                    + answer;
            out.println(outputLine);
            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    private static String executeBinary(String[] parameters) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class myClass = Class.forName(parameters[0]);
        Class<?> typeFirstPar = getType(parameters[2]);
        Class<?> typeSecondPar = getType(parameters[4]);
        Method methodToInvoke = myClass.getMethod(parameters[1], typeFirstPar, typeSecondPar);
        String firstType = parameters[2];
        String secondType = parameters[4];
        Object response = null;
        if (firstType.equals("String") && secondType.equals("String")) {
            response = methodToInvoke.invoke(null, parameters[3].substring(1, parameters[3].length()-1), parameters[5].substring(1, parameters[5].length()-1));
        } else if (firstType.equals("String") && secondType.equals("int")) {
            response = methodToInvoke.invoke(null, parameters[3].substring(1, parameters[3].length()-1), Integer.parseInt(parameters[5]));
        } else if (firstType.equals("String") && secondType.equals("double")) {
            response = methodToInvoke.invoke(null, parameters[3].substring(1, parameters[3].length()-1), Double.parseDouble(parameters[5]));
        } else if (firstType.equals("int") && secondType.equals("String")) {
            response = methodToInvoke.invoke(null, Integer.parseInt(parameters[3]), parameters[5].substring(1, parameters[5].length()-1));
        } else if (firstType.equals("double") && secondType.equals("String")) {
            response = methodToInvoke.invoke(null, Double.parseDouble(parameters[3]), parameters[5].substring(1, parameters[5].length()-1));
        } else if (firstType.equals("double") && secondType.equals("int")) {
            response = methodToInvoke.invoke(null, Double.parseDouble(parameters[3]), Integer.parseInt(parameters[5]));
        } else if (firstType.equals("int") && secondType.equals("double")) {
            response = methodToInvoke.invoke(null, Integer.parseInt(parameters[3]), Double.parseDouble(parameters[5]));
        } else if (firstType.equals("int") && secondType.equals("int")) {
            response = methodToInvoke.invoke(null, Integer.parseInt(parameters[3]), Integer.parseInt(parameters[5]));
        } else if (firstType.equals("double") && secondType.equals("double")) {
            response = methodToInvoke.invoke(null, Double.parseDouble(parameters[3]), Double.parseDouble(parameters[5]));
        }
        return response.toString();
    }

    private static String executeUniray(String[] parameters) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class myClass = Class.forName(parameters[0]);
        Class<?> typeFirstPar = getType(parameters[2]);
        Method methodToInvoke = myClass.getMethod(parameters[1], typeFirstPar);
        String firstType = parameters[2];
        Object response = null;
        if (firstType.equals("int")) {
            response = methodToInvoke.invoke(null, Integer.parseInt(parameters[3]));
        } else if (firstType.equals("double")) {
            response = methodToInvoke.invoke(null, Double.parseDouble(parameters[3]));
        } else if (firstType.equals("String")) {
            response = methodToInvoke.invoke(null, parameters[3].substring(1, parameters[3].length()-1));
        }
        return response.toString();
    }

    private static String executeInvoke(String[] parameters) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class myClass = Class.forName(parameters[0]);
        Method methodToInvoke = myClass.getMethod(parameters[1]);
        Object response = methodToInvoke.invoke(null);
        return response.toString();
    }

    private static Class<?> getType (String type) {
        if (type.equals("int")) {
            return Integer.TYPE;
        } else if (type.equals("double")) {
            return Double.TYPE;
        } else if (type.equals("String")) {
            return String.class;
        }
        return null;
    }

    private static String[] separateParameters (String parametersFromMehtod) {
        String[] finalParameters = parametersFromMehtod.split(",");
        for (int i = 0; i < finalParameters.length; i++) {
            if (i != 0) {
                finalParameters[i] = finalParameters[i].substring(1);
            }
        }
        return finalParameters;
    }
}
