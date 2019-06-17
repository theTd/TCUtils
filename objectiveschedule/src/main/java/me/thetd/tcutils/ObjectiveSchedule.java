package me.thetd.tcutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.totemcraftmc.bukkitplugin.TCBaseLib.command.AbstractCommandExecutor;
import org.totemcraftmc.bukkitplugin.TCBaseLib.command.RootCommand;
import org.totemcraftmc.bukkitplugin.TCBaseLib.command.TabCompletable;
import org.totemcraftmc.bukkitplugin.TCBaseLib.command.TabResponse;
import org.totemcraftmc.bukkitplugin.TCBaseLib.util.FormatUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ObjectiveSchedule extends TCUtilsModule {
    private final Pattern playerNamePattern = Pattern.compile("^[A-Za-z0-9_]{3,17}$");
    private final SimpleDateFormat alignDateFormat = new SimpleDateFormat("HH:mm:ss");
    private final ObjectiveScheduleCommand command = new ObjectiveScheduleCommand();
    private final Timer timer = new Timer("ObjectiveSchedule Timer");
    private final List<ScheduleElement> scheduleElements = new ArrayList<>();

    public ObjectiveSchedule() {
        super("ObjectiveSchedule");
    }

    @Override
    protected void load() {
        command.register(getPlugin());

        File confFile = new File(getPlugin().getDataFolder() + File.separator + "ObjectiveSchedules.yml");
        YamlConfiguration conf = new YamlConfiguration();
        if (confFile.exists()) {
            try {
                conf.load(confFile);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

        //noinspection unchecked
        List<Map> list = (List<Map>) conf.getList("schedules");
        if (list != null) {
            for (Map map : list) {
                ScheduleElement element = readElementFromMap(map);
                scheduleElements.add(element);
            }
        }
        getPlugin().getLogger().info(String.format("已读入 %d 个定时任务", scheduleElements.size()));
    }

    @Override
    public void unload() {
        scheduleElements.forEach(ScheduleElement::cancel);
        scheduleElements.clear();
        command.unregister();
    }

    private void save() throws IOException {
        YamlConfiguration conf = new YamlConfiguration();
        List<Map> mapList = new ArrayList<>();
        for (ScheduleElement ele : scheduleElements) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("objective", ele.objective);
            map.put("period", ele.period);
            map.put("mode", ele.mode.name());
            map.put("amount", ele.amount);
            map.put("align", ele.align);
            mapList.add(map);
        }
        conf.set("schedules", mapList);
        File confFile = new File(getPlugin().getDataFolder() + File.separator + "ObjectiveSchedules.yml");
        if (!confFile.exists() && !confFile.createNewFile()) throw new RuntimeException("failed creating file");
        conf.save(confFile);
    }

    private class ObjectiveScheduleCommand extends RootCommand {

        ObjectiveScheduleCommand() {
            super("objectiveSchedule", true, true);
            registerSubCommand(new ListCommand());
            registerSubCommand(new SetCommand());
            registerSubCommand(new RemoveCommand());
        }

        @Override
        protected boolean onCall(boolean playerRun, CommandSender sender, Player player, String[] args) {
            if (args.length != 0) return false;
            getSubCommands().stream().filter(exec -> exec.getPermission() == null || sender.hasPermission(exec.getPermission()))
                    .forEach(exec -> msg(exec.getUsage() + " - " + exec.getDescription()));
            return true;
        }

        private class ListCommand extends AbstractCommandExecutor {
            ListCommand() {
                super("list", ObjectiveScheduleCommand.this);
            }

            @Override
            protected boolean onCall(boolean playerRun, CommandSender sender, Player player, String[] args) {
                if (args.length != 0) return false;
                msg(String.format("正在列出 %d 个已设置的定时任务", scheduleElements.size()));
                for (int i = 0; i < scheduleElements.size(); i++) {
                    ScheduleElement ele = scheduleElements.get(i);
                    msg(i + String.format(" >>> 对 %20s 每 %s %s %d 对齐 %s",
                            ele.objective, FormatUtil.formatTime(ele.period), ele.mode.name(), ele.amount, alignDateFormat.format(new Date(ele.align))));
                }
                return true;
            }
        }

        private class SetCommand extends AbstractCommandExecutor implements TabCompletable {
            SetCommand() {
                super("set", ObjectiveScheduleCommand.this);
            }

            @Override
            protected boolean onCall(boolean playerRun, CommandSender sender, Player player, String[] args) {
                if (args.length > 5 || args.length < 4) return false;
                try {
                    String objective = args[0];
                    int period = Integer.parseInt(args[1]);
                    Mode mode = Mode.valueOf(args[2]);
                    int amount = Integer.parseInt(args[3]);
                    long align = System.currentTimeMillis();
                    if (args.length == 5) {
                        align = alignDateFormat.parse(args[4]).getTime();
                    }
                    scheduleElements.add(new ScheduleElement(objective, period, mode, amount, align));
                    save();
                    msg(ChatColor.GREEN + "ok");
                } catch (Exception e) {
                    msg(ChatColor.RED + e.getMessage());
                }
                return true;
            }


            @Override
            public TabResponse onTabComplete(boolean playerRun, CommandSender sender, Player player, String[] args) {
                if (args.length > 5) return new TabResponse();
                if (args.length <= 1) {
                    String pre = args.length == 0 ? "" : args[0].toLowerCase();
                    return new TabResponse(Bukkit.getScoreboardManager().getMainScoreboard().getObjectives().stream().filter(obj -> obj.getName().toLowerCase().startsWith(pre)).map(Objective::getName).collect(Collectors.toList()));
                }
                if (args.length <= 2) return new TabResponse();
                if (args.length <= 3) {
                    String pre = args.length == 0 ? "" : args[2].toLowerCase();
                    return new TabResponse(Arrays.stream(Mode.values()).filter(mode -> mode.name().toLowerCase().startsWith(pre)).map(Mode::name).collect(Collectors.toList()));
                }
                return new TabResponse();
            }
        }

        private class RemoveCommand extends AbstractCommandExecutor {
            RemoveCommand() {
                super("remove", ObjectiveScheduleCommand.this);
            }

            @Override
            protected boolean onCall(boolean playerRun, CommandSender sender, Player player, String[] args) {
                if (args.length != 1) return false;
                try {
                    ScheduleElement ele = scheduleElements.remove(Integer.parseInt(args[0]));
                    ele.cancel();
                    save();
                    msg(ChatColor.GREEN + "ok");
                } catch (Exception e) {
                    msg(ChatColor.RED + e.getMessage());
                }
                return true;
            }
        }
    }

    private class ScheduleElement {
        private final Task task = new Task();
        private final String objective;
        private final int period;
        private final Mode mode;
        private final int amount;
        private final long align;

        private ScheduleElement(String objective, int period, Mode mode, int amount, long align) {
            this.objective = objective;
            this.period = period;
            this.mode = mode;
            this.amount = amount;
            this.align = align;
            timer.scheduleAtFixedRate(task, align(), this.period * 1000);
        }

        private void cancel() {
            task.cancel();
        }

        private long align() {
            long d = (1000 - (Math.abs(System.currentTimeMillis() - align) % 1000));
            return d > 0 ? d : 1000 + d;
        }

        private class Task extends TimerTask {

            @Override
            public void run() {
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective obj = scoreboard.getObjective(objective);
                    if (obj == null) {
                        getPlugin().getLogger().warning("cannot find a objective named " + objective);
                        return;
                    }

                    for (String entry : scoreboard.getEntries()) {
                        if (!playerNamePattern.matcher(entry).find()) continue;
                        Score score = obj.getScore(entry);
                        switch (mode) {
                            case ADD:
                                if (score.isScoreSet()) {
                                    score.setScore(score.getScore() + amount);
                                } else {
                                    score.setScore(amount);
                                }
                                break;
                            case REMOVE:
                                if (!score.isScoreSet()) {
                                    score.setScore(0);
                                } else {
                                    int tmp = score.getScore() - amount;
                                    tmp = tmp < 0 ? 0 : tmp;
                                    if (score.getScore() != tmp) score.setScore(tmp);
                                }
                                break;
                            case SET:
                                score.setScore(amount);
                                break;
                        }
                    }
                });
            }
        }
    }

    private enum Mode {
        ADD, REMOVE, SET
    }

    private ScheduleElement readElementFromMap(Map map) {
        String objective = (String) map.get("objective");
        int period = (int) map.get("period");
        Mode mode = Mode.valueOf((String) map.get("mode"));
        int amount = (int) map.get("amount");
        Object alignObj = map.get("align");
        long align = (alignObj instanceof Integer) ? ((long) (int) alignObj) : (long) alignObj;
        return new ScheduleElement(objective, period, mode, amount, align);
    }
}
