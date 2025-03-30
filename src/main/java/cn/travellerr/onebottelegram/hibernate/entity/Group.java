package cn.travellerr.onebottelegram.hibernate.entity;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "`Group`")
@Entity
public class Group {
    @Id
    private Long groupId;

    private String groupName;

    private String groupDescription;

    private int memberCount;

    @Builder.Default
    private int maxMemberCount = 2000;

    @Column(length = 4096)
    private String memberIds;

    @Column(length = 4096)
    private String memberUserNames;

    @Transient
    private List<Long> membersIdList;

    @Transient
    private List<String> membersUserNameList;




    public List<Long> getMembersIdList() {
        this.membersIdList = new ArrayList<>();
        if (StrUtil.isNotBlank(memberIds)) {
            for (String s : memberIds.split(",")) {
                membersIdList.add(Long.parseLong(s));
            }
        } else {
            membersIdList = new ArrayList<>();
        }
        return membersIdList;
    }

    public void setMembersId(List<Long> membersIdList) {
        this.membersIdList = membersIdList;
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public void addMemberId(Long member) {
        this.membersIdList = getMembersIdList();
        if (membersIdList!= null && membersIdList.contains(member)) {
            return;
        }
        membersIdList.add(member);
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        HibernateFactory.merge(this);
    }

    public void removeMemberId(Long member) {
        this.membersIdList = getMembersIdList();
        membersIdList.remove(member);
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }


    public List<String> getMemberUsernamesList() {
        this.membersUserNameList = new ArrayList<>();
        if (StrUtil.isNotBlank(memberUserNames)) {
            membersUserNameList.addAll(Arrays.asList(memberUserNames.split(",")));
        } else {
            membersUserNameList = new ArrayList<>();
        }
        return membersUserNameList;
    }

    public void setMemberUsernames(List<String> membersUserNameList) {
        this.membersUserNameList = membersUserNameList;
        this.memberUserNames = membersUserNameList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public void addMemberUsernames(String member) {
        this.membersUserNameList = getMemberUsernamesList();
        if (membersUserNameList!= null && membersUserNameList.contains(member)) {
            return;
        }
        membersUserNameList.add(member);
        System.out.println("new member was found: " + member);
        this.memberUserNames = membersUserNameList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        HibernateFactory.merge(this);
    }

    public void removeMemberUsernames(String member) {
        this.membersUserNameList = getMemberUsernamesList();
        membersUserNameList.remove(member);
        this.memberUserNames = membersUserNameList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }
}
