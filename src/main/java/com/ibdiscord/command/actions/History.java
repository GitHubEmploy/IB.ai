/* Copyright 2020 Nathaneal Varghese
 *
 * This file is part of IB.ai.
 *
 * IB.ai is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IB.ai is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IB.ai. If not, see http://www.gnu.org/licenses/.
 */

package com.ibdiscord.command.actions;

import com.ibdiscord.command.CommandAction;
import com.ibdiscord.command.CommandContext;
import com.ibdiscord.data.db.DataContainer;
import com.ibdiscord.data.db.entries.punish.PunishmentData;
import com.ibdiscord.data.db.entries.punish.PunishmentsData;
import com.ibdiscord.punish.Punishment;
import com.ibdiscord.punish.PunishmentHandler;
import de.arraying.gravity.Gravity;
import de.arraying.gravity.data.property.Property;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.ibdiscord.data.db.entries.punish.PunishmentData.MESSAGE;

public final class History implements CommandAction {

    /**
     * Returns history of user.
     * @param context The command context.
     */
    @Override
    public void accept(CommandContext context) {
        context.assertArguments(1, "error.missing_member");
        Member member = context.assertMemberArgument("error.note_invalid");

        Guild guild = context.getGuild();
        Gravity gravity = DataContainer.INSTANCE.getGravity();
        PunishmentsData punishmentList = gravity.load(new PunishmentsData(guild.getId()));

        List<Long> caseIds = punishmentList.values().stream()
                .filter(caseId -> Punishment.of(guild, caseId).getUserId().equals(member.getId()))
                .map(Property::asLong)
                .collect(Collectors.toList());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        caseIds.forEach((caseId) -> {
            Punishment punishment = Punishment.of(guild, caseId);
            PunishmentData punishmentData = gravity.load(new PunishmentData(guild.getId(), caseId));
            PunishmentHandler punishmentHandler = new PunishmentHandler(guild, punishment);

            embedBuilder.setTitle(String.format("History Of %s",
                    member != null ? member.getUser().getAsTag()
                            : !punishment.isRedacted() ? punishment.getUserDisplay()
                            : context.getArguments()[0]));

            TextChannel channel = punishmentHandler.getLogChannel();
            if(channel == null) {
                context.replyI18n("error.reason_logging");
                return;
            }

            Message message  = channel
                    .retrieveMessageById(punishmentData.get(MESSAGE).defaulting(0L).asLong())
                    .complete();

            String date = "???";

            if (message != null) {
                date = message.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/YYY"));
            }

            embedBuilder.addField(String.format("Case %s (%s) - By %s", caseId,
                    date,
                    punishment.getStaffDisplay()),
                    String.format("%s - %s", punishment.getType().getDisplayInitial(),
                            punishment.getReason()), false);
        });
        context.replyEmbed(embedBuilder.build());
    }
}