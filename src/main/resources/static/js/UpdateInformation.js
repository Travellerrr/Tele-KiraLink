// 侧边栏切换逻辑
let isCollapsed = false;
document.getElementById('toggle-btn').addEventListener('click', () => {
    const sidebar = document.getElementById('sidebar');
    isCollapsed = !isCollapsed;
    sidebar.style.marginLeft = isCollapsed ? '-280px' : '0';
    document.getElementById('toggle-btn').innerHTML = isCollapsed ? '▶' : '◀';
});

async function fetchBotInfo(isFirst) {
    try {
        const response = await fetch(`/api/bot-info${isFirst ? '?withAvatar=1' : ''}`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const botInfo = await response.json();

        document.querySelector('.bot-info h2').textContent = botInfo.firstName;
        document.querySelector('.bot-info h4').textContent = botInfo.name;
        if (isFirst) {
            document.querySelector('.avatar').src = botInfo.avatarUrl.startsWith("file:///")
                ? await fetch(botInfo.avatarUrl)
                    .then(res => res.blob())
                    .then(blob => new Promise((resolve, reject) => {
                        const reader = new FileReader();
                        reader.onloadend = () => resolve(reader.result);
                        reader.onerror = reject;
                        reader.readAsDataURL(blob);
                    }))
                : botInfo.avatarUrl;
        }
        document.getElementById('uptime').textContent = botInfo.uptime;
    } catch (error) {
        console.error('获取机器人信息失败:', error);
        document.getElementById('latency').textContent = '--';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    fetchBotInfo(true);
    setInterval(() => fetchBotInfo(false), 5000);
});

function toggleMenu() {
    document.getElementById('menuContent').classList.toggle('show');
}

async function selectFunction(type) {
    const contactList = document.getElementById('contact-list');
    if (type === 'message') {
        contactList.style.display = 'block';
        try {
            const response = await fetch('/api/bot-contacts');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const contacts = await response.json();
            contactList.innerHTML = contacts.map(contact => `
                <button onclick="setContact(${contact.groupId}, '${contact.groupName}')" style="width: 100%; text-align: left; margin-bottom: 10px;">
                    <ul style="list-style: none; padding: 0; margin: 0;">
                        <li><strong>Group Name:</strong> ${contact.groupName}</li>
                        <li><strong>Group ID:</strong> ${contact.groupId}</li>
                    </ul>
                </button>
            `).join('');
            contactList.style.maxHeight = '150px';
            contactList.style.overflowY = 'auto';
        } catch (error) {
            console.error('Error fetching contacts:', error);
        }
    } else {
        contactList.style.display = 'none';
    }
    toggleMenu();
}

let selectedContact = 0, selectedContactName = '';

async function sendMessage() {
    const input = document.getElementById('input-field');
    const log = document.getElementById('log-container');
    if (input.value.trim() === '') {
        return;
    }
    if (selectedContact !== 0) {
        if (await sendPostRequest(selectedContact, input.value)) {
            log.innerHTML += `<div class="log-entry">[发送至 ${selectedContactName}] ${new Date().toLocaleTimeString()} - ${input.value}</div>`;
        } else {
            log.innerHTML += `<div class="log-entry">[发送失败！] ${new Date().toLocaleTimeString()} - ${input.value}</div>`;
        }
        input.value = '';
        log.scrollTop = log.scrollHeight;
        return;
    }
    log.innerHTML += `<div class="log-entry">[发送] ${new Date().toLocaleTimeString()} - ${input.value}</div>`;
    input.value = '';
    log.scrollTop = log.scrollHeight;
}

function setContact(groupId, groupName) {
    selectedContact = groupId;
    selectedContactName = groupName;
    document.getElementById('contact-list').style.display = 'none';
}

async function sendPostRequest(contactId, msg) {
    try {
        const response = await fetch('/api/bot-contacts/send-msg', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // 启用 Cookie
            body: JSON.stringify({ contactId, msg, isGroup: true })
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error('Error sending POST request:', error);
    }
}

const ws = new WebSocket(`ws://${location.host}/ws/logs`);
ws.onmessage = function(event) {
    const logContainer = document.getElementById('log-container');
    const shouldScroll = logContainer.scrollTop + logContainer.clientHeight === logContainer.scrollHeight;
    const newEntry = document.createElement('div');
    newEntry.className = 'log-entry';
    newEntry.textContent = event.data;
    logContainer.appendChild(newEntry);
    if (shouldScroll) logContainer.scrollTop = logContainer.scrollHeight;
};