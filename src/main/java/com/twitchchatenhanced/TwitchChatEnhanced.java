package com.twitchchatenhanced;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import com.mojang.brigadier.arguments.StringArgumentType;

public class TwitchChatEnhanced implements ClientModInitializer {
 private static final Path CONFIG = Path.of("config", "twitch-chat-enhanced.properties");
 private final AtomicBoolean running = new AtomicBoolean(false);
 private volatile Socket socket;
 private volatile String channel = "";
 private volatile boolean mentions = true;
 private volatile boolean enabled = true;
 private volatile String nickname = "";

 @Override public void onInitializeClient() {
  load();
  ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("twitch")
   .then(literal("join").then(argument("channel", StringArgumentType.word()).executes(ctx -> {
    channel = StringArgumentType.getString(ctx, "channel").replace("#", "").toLowerCase(Locale.ROOT);
    save(); restart(); notice("A ligar ao canal #" + channel); return 1;
   })))
   .then(literal("leave").executes(ctx -> { channel = ""; save(); stop(); notice("Desligado."); return 1; }))
   .then(literal("on").executes(ctx -> { enabled = true; save(); restart(); notice("Ativado."); return 1; }))
   .then(literal("off").executes(ctx -> { enabled = false; save(); stop(); notice("Desativado."); return 1; }))
   .then(literal("mentions").then(literal("on").executes(ctx -> { mentions = true; save(); notice("Menções ativadas."); return 1; }))
      .then(literal("off").executes(ctx -> { mentions = false; save(); notice("Menções desativadas."); return 1; })))
   .then(literal("nick").then(argument("name", StringArgumentType.word()).executes(ctx -> {
     nickname = StringArgumentType.getString(ctx, "name"); save(); notice("Nome para menções: " + nickname); return 1;
   })))
   .then(literal("status").executes(ctx -> { notice("Canal: " + (channel.isEmpty()?"nenhum":channel) + " | " + (running.get()?"ligado":"desligado")); return 1; }))
  ));
  if (enabled && !channel.isEmpty()) restart();
 }
 private void restart() { stop(); if (enabled && !channel.isEmpty()) { running.set(true); new Thread(this::connectLoop, "TwitchChat-TLS").start(); } }
 private void stop() { running.set(false); try { if (socket != null) socket.close(); } catch (IOException ignored) {} }
 private void connectLoop() {
  String wanted = channel;
  while (running.get() && wanted.equals(channel)) {
   try (Socket s = SSLSocketFactory.getDefault().createSocket("irc.chat.twitch.tv", 6697)) {
    socket = s; s.setSoTimeout(120000);
    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
    String guest = "justinfan" + (10000 + new java.security.SecureRandom().nextInt(80000));
    send(out, "CAP REQ :twitch.tv/tags twitch.tv/commands");
    send(out, "NICK " + guest);
    send(out, "JOIN #" + wanted);
    notice("Ligado ao chat #" + wanted);
    String line;
    while (running.get() && wanted.equals(channel) && (line = in.readLine()) != null) {
     if (line.startsWith("PING ")) { send(out, "PONG " + line.substring(5)); continue; }
     int pos = line.indexOf(" PRIVMSG #");
     if (pos < 0) continue;
     int body = line.indexOf(" :", pos);
     if (body < 0) continue;
     String msg = line.substring(body + 2);
     String prefix = line.substring(0, pos);
     int bang = prefix.indexOf('!');
     String sender = bang > 0 ? prefix.substring(prefix.lastIndexOf(':', bang) + 1, bang) : "twitch";
     if (msg.length() > 500) msg = msg.substring(0, 500);
     final String finalMsg = msg, finalSender = sender;
     display("[Twitch] <" + finalSender + "> " + finalMsg);
     if (mentions && !nickname.isBlank() && msg.toLowerCase(Locale.ROOT).contains(nickname.toLowerCase(Locale.ROOT)))
      notice("Mencionaram-te no chat da Twitch!");
    }
   } catch (Exception e) { if (running.get()) notice("Ligação interrompida; a tentar novamente."); }
   if (running.get() && wanted.equals(channel)) try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
  }
 }
 private static void send(BufferedWriter out, String line) throws IOException { out.write(line); out.write("\r\n"); out.flush(); }
 private static void display(String message) {
  Minecraft mc = Minecraft.getInstance();
  mc.execute(() -> { if (mc.player != null) mc.player.displayClientMessage(Component.literal(message), false); });
 }
 private static void notice(String message) { display("[Twitch] " + message); }
 private void load() {
  Properties p = new Properties();
  try { if (Files.exists(CONFIG)) try (Reader r = Files.newBufferedReader(CONFIG)) { p.load(r); } } catch (IOException ignored) {}
  channel = p.getProperty("channel", "").replaceAll("[^A-Za-z0-9_]", "").toLowerCase(Locale.ROOT);
  nickname = p.getProperty("nickname", "").replaceAll("[^A-Za-z0-9_]", "");
  mentions = Boolean.parseBoolean(p.getProperty("mentions", "true"));
  enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
 }
 private void save() {
  Properties p = new Properties(); p.setProperty("channel", channel); p.setProperty("nickname", nickname);
  p.setProperty("mentions", Boolean.toString(mentions)); p.setProperty("enabled", Boolean.toString(enabled));
  try { Files.createDirectories(CONFIG.getParent()); try (Writer w = Files.newBufferedWriter(CONFIG)) { p.store(w, "Twitch Chat Enhanced settings"); } } catch (IOException ignored) {}
 }
}
