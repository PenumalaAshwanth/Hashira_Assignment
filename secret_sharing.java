// SecretSharing.java
// Compile with: javac -cp gson-2.10.1.jar SecretSharing.java
// Run with: java -cp .:gson-2.10.1.jar SecretSharing test1.json test2.json
// (On Windows, replace ':' with ';' in the classpath)

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.google.gson.*;  // Add gson-2.x.jar to your classpath

public class SecretSharing {

    private static BigInteger parseBigIntFromBase(String value, int base) {
        return new BigInteger(value, base);
    }

    private static BigInteger lagrangeConstant(List<BigInteger[]> points, int k) {
        BigInteger num = BigInteger.ZERO;
        BigInteger den = BigInteger.ONE;

        for (int i = 0; i < k; i++) {
            BigInteger xi = points.get(i)[0];
            BigInteger yi = points.get(i)[1];

            BigInteger liNum = BigInteger.ONE;
            BigInteger liDen = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = points.get(j)[0];
                liNum = liNum.multiply(xj.negate());
                liDen = liDen.multiply(xi.subtract(xj));
            }

            num = num.multiply(liDen).add(yi.multiply(liNum).multiply(den));
            den = den.multiply(liDen);

            BigInteger g = num.gcd(den);
            num = num.divide(g);
            den = den.divide(g);
        }

        if (!den.equals(BigInteger.ONE)) {
            throw new RuntimeException("Result is not an integer!");
        }
        return num;
    }

    private static BigInteger processFile(String filename) throws IOException {
        String raw = new String(Files.readAllBytes(Paths.get(filename)));
        JsonObject data = JsonParser.parseString(raw).getAsJsonObject();

        int n = data.getAsJsonObject("keys").get("n").getAsInt();
        int k = data.getAsJsonObject("keys").get("k").getAsInt();

        List<Integer> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            if (!entry.getKey().equals("keys")) {
                keys.add(Integer.parseInt(entry.getKey()));
            }
        }
        Collections.sort(keys);

        List<BigInteger[]> points = new ArrayList<>();
        for (int x : keys) {
            JsonObject obj = data.getAsJsonObject(String.valueOf(x));
            int base = Integer.parseInt(obj.get("base").getAsString());
            String valStr = obj.get("value").getAsString();
            BigInteger y = parseBigIntFromBase(valStr, base);
            points.add(new BigInteger[]{BigInteger.valueOf(x), y});
        }

        if (points.size() < k) {
            throw new RuntimeException("Not enough points in " + filename);
        }

        return lagrangeConstant(points, k);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java SecretSharing test1.json test2.json ...");
            return;
        }

        for (String file : args) {
            BigInteger secret = processFile(file);
            System.out.println(file + ": " + secret);
        }
    }

    /*
    Expected Output (after running with the provided test JSON files):

    test1.json: 3
    test2.json: 271644355478965
    */
}