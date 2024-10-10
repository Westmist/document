// .vuepress/config.js
module.exports = {
  // 网站icon
  head: [["link", { rel: "icon", href: "/logo.png" }]],
  title: "明轩手札",
  description: "明轩的个人笔记博客",
  themeConfig: {
    port: 8000,

    // 导航栏
    nav: [
      { text: "Home", link: "/" },
      { text: "Guide", link: "https://docs.oracle.com/javase/8/docs/api/" },
      { text: "External", link: "https://vuepress.vuejs.org/" },
      { text: "Test", link: "/page/ansible.md" },
      // 导航栏下拉列表
      {
        text: "Languages",
        ariaLabel: "Language Menu",
        items: [
          { text: "Chinese", link: "/language/chinese/" },
          { text: "Japanese", link: "/language/japanese/" },
        ],
      },
    ],

    // 侧边栏
    sidebar: [
        {
          title: 'Ansible',
          path: '/page/ansible.md',
          collapsable: false, // 可选的, 默认值是 true,
          sidebarDepth: 3,    // 可选的, 默认值是 1
        //   children: [
        //     '/page-a/abc'
        //   ]
        },
        {
          title: 'Docker',
          path: '/page/docker.md',
          initialOpenGroupIndex: -1 // 可选的, 默认值是 0
        },
        {
          title: 'Vuepress',
          path: '/page/vuepress.md',
        },
        // {
        //   title: 'Arthas',
        //   path: '/page/arthas.md',
        // },
        {
          title: 'Jenkins',
          path: '/page/jenkins.md',
        }
    ],
    

    // 侧边栏
    // sidebar: ["/page/ansible.md"],

    // 其它配置
    markdown: {
      // 显示代码块行号
      lineNumbers: true,
    },
  },

  configureWebpack: {
    resolve: {
      alias: {
        '@test': '/test'
      }
    }
  }

};
