package cn.travellerr.onebottelegram.hibernate.entity;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    private String memberIds;

    @Transient
    private List<Long> membersIdList;




    public List<Long> getMembersIdList() {
        this.membersIdList = new ArrayList<>();
        if (StrUtil.isNotBlank(memberIds)) {
            for (String s : memberIds.split(",")) {
                membersIdList.add(Long.parseLong(s));
            }
        }
        return membersIdList;
    }

    public void setMembersId(List<Long> membersIdList) {
        this.membersIdList = membersIdList;
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public void addMember(Long member) {
        this.membersIdList = getMembersIdList();
        membersIdList.add(member);
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        HibernateFactory.merge(this);
    }

    public void removeMember(Long member) {
        this.membersIdList = getMembersIdList();
        membersIdList.remove(member);
        this.memberIds = membersIdList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

}
