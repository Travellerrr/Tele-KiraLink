// 修改顶部侧边栏切换逻辑
let isMobile = window.matchMedia("(max-width: 50em)").matches;
let sidebar = document.getElementById('sidebar');
// let toggleBtn = document.getElementById('toggle-btn');

// 统一管理侧边栏状态
function toggleSidebar() {
    if(isMobile) {
        sidebar.classList.toggle('active');
        document.body.style.overflow = sidebar.classList.contains('active') ? 'hidden' : '';
        toggleBtn.innerHTML = sidebar.classList.contains('active') ? '▼' : '▲';
    } else {
        sidebar.classList.toggle('active');
        sidebar.style.marginLeft = sidebar.classList.contains('active')  ? '-280px' : '0';
        toggleBtn.innerHTML = sidebar.classList.contains('active')  ? '▶' : '◀';
    }
}

// 响应式检测
window.addEventListener('resize', () => {
    isMobile = window.matchMedia("(max-width: 60rem)").matches;
    toggleBtn.innerHTML = sidebar.classList.contains('active') ? (isMobile ? '▼' : '▶') : (isMobile ? '▲' : '◀');
});

// 修改原有事件监听
toggleBtn.addEventListener('click', toggleSidebar);

// 防止滚动穿透
document.body.addEventListener('touchmove', (e) => {
    if(sidebar.classList.contains('active')) {
        e.preventDefault();
    }
}, { passive: false });

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
    fetchChatHistory();
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
    } else if(type === 'quick') {
        const commands = "Available commands: test, reload, help, stop, cleanChatHistory, gc"
            .replace("Available commands: ", "")
            .split(", ")
            .map(command => command.trim());
        const contactList = document.getElementById('contact-list');
        contactList.style.display = 'block';
        contactList.innerHTML = commands.map(command => `
            <button onclick="quickCommand('${command}')" style="width: 100%; text-align: left; margin-bottom: 10px;">
                <ul style="list-style: none; padding: 0; margin: 0;">
                    <li><strong>Command:</strong> ${command}</li>
                </ul>
            </button>
        `).join('');
    } else {
        selectedContact = -1;
        contactList.style.display = 'none';
    }
    toggleMenu();
}

async function quickCommand(command) {
    const log = document.getElementById('log-container');
    const map = new Map();
    map.set("command", command);
    const response = await sendPostRequest(map, '/api/toa/command');
    log.innerHTML += `<div class="log-entry">[发送] ${new Date().toLocaleTimeString()} - ${command}</div>`;
    log.innerHTML += `<div class="log-entry">[返回] ${new Date().toLocaleTimeString()} - ${response}</div>`;
    log.scrollTop = log.scrollHeight;
}

let selectedContact = -1, selectedContactName = '';

async function sendMessage() {
    const input = document.getElementById('input-field');
    const log = document.getElementById('log-container');
    if (input.value.trim() === '') {
        return;
    }
    let response;
    let value = input.value.trim();

    input.value = '';
    if (selectedContact !== -1) {
        const map = new Map();
        map.set('contactId', selectedContact);
        map.set("msg", value);
        map.set("isGroup", true)
        if (await sendPostRequest(map, '/api/bot-contacts/send-msg')) {
            log.innerHTML += `<div class="log-entry">[发送至 ${selectedContactName}] ${new Date().toLocaleTimeString()} - ${value}</div>`;
        } else {
            log.innerHTML += `<div class="log-entry">[发送失败！] ${new Date().toLocaleTimeString()} - ${value}</div>`;
        }

    } else {
        const map = new Map();
        map.set("command", value);
        response = await sendPostRequest(map, 'api/toa/command');

        log.innerHTML += `<div class="log-entry">[发送] ${new Date().toLocaleTimeString()} - ${value}</div>`;
        log.innerHTML += `<div class="log-entry">[返回] ${new Date().toLocaleTimeString()} - ${response}</div>`;
    }
    input.value = '';
    log.scrollTop = log.scrollHeight;
}

function setContact(groupId, groupName) {
    selectedContact = groupId;
    selectedContactName = groupName;
    document.getElementById('contact-list').style.display = 'none';
}

async function sendPostRequest(data, url) {
    try {
        console.log(JSON.stringify(Object.fromEntries(data)));
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // 启用 Cookie
            body: JSON.stringify(Object.fromEntries(data))
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.text();
    } catch (error) {
        console.error('Error sending POST request:', error);
    }
}

let ws;

function connectWebSocket() {
    ws = new WebSocket(`ws://${location.host}/ws/logs`);

    ws.onopen = function() {
        console.log('WebSocket connection established.');
    };

    ws.onmessage = function(event) {
        const logContainer = document.getElementById('log-container');
        const shouldScroll = logContainer.scrollTop + logContainer.clientHeight === logContainer.scrollHeight;
        const newEntry = document.createElement('div');
        newEntry.className = 'log-entry';
        newEntry.textContent = event.data;
        logContainer.appendChild(newEntry);
        if (shouldScroll) {
            logContainer.scrollTop = logContainer.scrollHeight;
        }
    };

    ws.onclose = function() {
        console.warn('WebSocket connection closed. Attempting to reconnect...');
        setTimeout(connectWebSocket, 5000); // Retry connection after 5 seconds
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
        ws.close();
    };
}

// Initialize WebSocket connection
connectWebSocket();

async function fetchChatHistory() {
    try {
        const response = await fetch('/api/toa/chat-history', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // 启用 Cookie
            body: JSON.stringify({ limit: 50 }),
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const history = await response.json();

        const logContainer = document.getElementById('log-container');
        history.forEach(entry => {
            const logEntry = document.createElement('div');
            logEntry.className = 'log-entry';
            logEntry.textContent = entry;
            logContainer.appendChild(logEntry);
        });
        logContainer.scrollTop = logContainer.scrollHeight;
    } catch (error) {
        console.error('Error fetching chat history:', error);
    }

}